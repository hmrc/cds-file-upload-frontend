/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import com.google.inject.Inject
import config.AppConfig
import models.{FileUploadRequest, FileUploadResponse}
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

trait CustomsDeclarationsConnector {

  def requestFileUpload(eori: String, request: FileUploadRequest)(implicit hc: HeaderCarrier): Future[FileUploadResponse]

}

class CustomsDeclarationsConnectorImpl @Inject()(
                                                  config: AppConfig,
                                                  httpClient: HttpClient)
                                                (implicit ec: ExecutionContext) extends CustomsDeclarationsConnector {

  private val fileUploadUrl = config.microservice.services.customsDeclarations.batchUploadEndpoint
  private val apiVersion    = config.microservice.services.customsDeclarations.apiVersion
  private val clientId      = config.developerHubClientId

  override def requestFileUpload(eori: String, request: FileUploadRequest)(implicit hc: HeaderCarrier): Future[FileUploadResponse] = {
    httpClient.POSTString(fileUploadUrl, request.toXml.mkString,  eoriHeader(eori) :: headers).map(r =>
      Try(XML.loadString(r.body)) match {
        case Success(value)     =>
          println(value); FileUploadResponse.fromXml(value)
        case Failure(exception) =>
          Logger.error(s"Failed to load XML with exception: ${exception.getMessage}")
          throw exception
      }
    )
  }

  private def eoriHeader(eori: String): (String, String) = "X-EORI-Identifier" -> eori

  private lazy val headers: List[(String, String)] =
    List(
      "X-Client-ID" -> clientId,
      HeaderNames.ACCEPT -> s"application/vnd.hmrc.$apiVersion+xml",
      HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
    )
}