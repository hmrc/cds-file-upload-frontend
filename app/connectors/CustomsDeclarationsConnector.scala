/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import models.{FileUploadRequest, FileUploadResponse}
import play.api.Logging
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.ws.writeableOf_String
import play.api.mvc.Codec
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class CustomsDeclarationsConnector @Inject() (appConfig: AppConfig, httpClientV2: HttpClientV2)(implicit ec: ExecutionContext)
    extends Connector with Logging {

  protected val httpClient: HttpClientV2 = httpClientV2

  private val fileUploadUrl = appConfig.microservice.services.customsDeclarations.batchUploadEndpoint
  private val apiVersion = appConfig.microservice.services.customsDeclarations.apiVersion
  private val clientId = appConfig.developerHubClientId

  def requestFileUpload(eori: String, request: FileUploadRequest)(implicit hc: HeaderCarrier): Future[FileUploadResponse] = {
    logger.info(s"Request to initiate ${request.toXml}")
    logger.info(s"fileUploadUrl: $fileUploadUrl")
    post[String, HttpResponse](fileUploadUrl, request.toXml.mkString, headers(eori))
      .map(response =>
        Try(XML.loadString(response.body)) match {
          case Success(value) =>
            logger.info(s"Got initiate response: $FileUploadResponse")
            FileUploadResponse.fromXml(value)

          case Failure(exception) =>
            logger.warn(s"Failed to load XML with exception: $exception")
            throw exception
        }
      )
  }

  private def headers(eori: String): Seq[(String, String)] = List(
    "X-Client-ID" -> clientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.$apiVersion+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-EORI-Identifier" -> eori
  )
}
