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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class CustomsDeclarationsConnector @Inject()(config: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val fileUploadUrl = config.microservice.services.customsDeclarations.batchUploadEndpoint
  private val apiVersion = config.microservice.services.customsDeclarations.apiVersion
  private val clientId = config.developerHubClientId

  def requestFileUpload(eori: String, request: FileUploadRequest)(implicit hc: HeaderCarrier): Future[FileUploadResponse] = {
    Logger.warn(s"REQUEST TO INITIATE ${request}")
    Logger.warn(s"REQUEST TO INITIATE ${request.toXml}")
    httpClient.POSTString[HttpResponse](fileUploadUrl, request.toXml.mkString, headers(eori)).map(r =>
      Try(XML.loadString(r.body)) match {
        case Success(value) =>
          Logger.warn(s"Got initiate response ${FileUploadResponse}")
          FileUploadResponse.fromXml(value)
        case Failure(exception) =>
          Logger.error("Failed to load XML")
          throw exception
      }
    )
  }

  private def headers(eori: String) = List(
    "X-Client-ID" -> clientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.$apiVersion+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-EORI-Identifier" -> eori
  )
}