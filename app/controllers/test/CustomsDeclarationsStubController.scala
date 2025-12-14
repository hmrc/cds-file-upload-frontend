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

package controllers.test

import config.AppConfig
import play.api.Logging
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.Files
import play.api.libs.ws.writeableOf_String
import play.api.mvc._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CustomsDeclarationsStubController @Inject() (appConfig: AppConfig, httpClientV2: HttpClientV2, mcc: MessagesControllerComponents)(
  implicit ec: ExecutionContext
) extends FrontendController(mcc) with Logging {

  val handleS3FileUploadRequest: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    val filename = request.body.files.head.filename
    val redirectLocation = redirectionAccordingTo(filename)
    val reference = redirectLocation.split("/").last
    callBack(filename, reference)
    SeeOther(redirectLocation).withHeaders("Location" -> redirectLocation)
  }

  private def redirectionAccordingTo(filename: String)(implicit request: MessagesRequest[MultipartFormData[_]]): String = {
    val isTestErrorPage = filename.toLowerCase.startsWith("test-error-page.")
    request.body.dataParts(if (isTestErrorPage) "error_action_redirect" else "success_action_redirect").head
  }

  private def callBack(filename: String, reference: String)(implicit hc: HeaderCarrier): Unit = {
    // Thread.sleep(1000)

    val outcome = if (filename.toLowerCase.startsWith("x")) "FAILURE" else "SUCCESS"

    val notification =
      <Root>
        <FileReference>{reference}</FileReference>
        <BatchId>5e634e09-77f6-4ff1-b92a-8a9676c715c4</BatchId>
        <FileName>{filename}</FileName>
        <Outcome>{outcome}</Outcome>
        <Details>[detail block]</Details>
      </Root>

    val cdsFileUploadConfig = appConfig.microservice.services.cdsFileUpload
    val cdsFileUploadBaseUrl = s"${cdsFileUploadConfig.protocol.get}://${cdsFileUploadConfig.host}:${cdsFileUploadConfig.port.get}"
    val url = cdsFileUploadBaseUrl + "/internal/notification"
    val header: (String, String) = HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)

    httpClientV2.post(url"$url").transform(_.addHttpHeaders(Seq(header): _*)).withBody[String](notification.toString).execute[HttpResponse]
    logger.warn(s"Sent notification for file ${reference} to ${url}")
  }
}
