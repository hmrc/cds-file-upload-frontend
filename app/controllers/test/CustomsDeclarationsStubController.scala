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

package controllers.test

import javax.inject.{Inject, Singleton}
import models.Field._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.mvc.{Action, AnyContent, MultipartFormData}
import services.NotificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext
import scala.xml._

@Singleton
class CustomsDeclarationsStubController @Inject()(notificationService: NotificationService)(implicit ec: ExecutionContext) extends FrontendController {

  case class UploadStuff(successActionRedirect: String)

  val form = Form(mapping(
    "success_action_redirect" -> nonEmptyText)
  (UploadStuff.apply)(UploadStuff.unapply)
  )

  var fileRef = 1
  def waiting(ref: String) = Waiting(UploadRequest(
    href = "http://localhost:6793/cds-file-upload-service/test-only/s3-bucket",
    fields = Map(
      Algorithm.toString -> "AWS4-HMAC-SHA256",
      Signature.toString -> "xxxx",
      Key.toString -> "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      ACL.toString -> "private",
      Credentials.toString -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
      Policy.toString -> "xxxxxxxx==",
      SuccessRedirect.toString -> s"http://localhost:6793/cds-file-upload-service/upload/upscan-success/${ref}",
      ErrorRedirect.toString -> s"http://localhost:6793/cds-file-upload-service/upload/upscan-error/${ref}"
    )
  ))

  // for now, we will just return some random
  def handleBatchFileUploadRequest: Action[NodeSeq] = Action(parse.xml) { implicit req =>
    fileRef = 1

    Thread.sleep(100)

    val fileGroupSize = (scala.xml.XML.loadString(req.body.mkString) \ "FileGroupSize").text.toInt

    val resp = FileUploadResponse((1 to fileGroupSize).map { i =>
      FileUpload(i.toString, waiting(i.toString), successUrl = RedirectUrl(s"http://localhost:6793/cds-file-upload-service/upload/upscan-success/$i"), errorUrl = RedirectUrl(s"http://localhost:6793/cds-file-upload-service/upload/upscan-error/$i"), id = s"$i")
    }.toList)

    Ok(XmlHelper.toXml(resp)).as(ContentTypes.XML)
  }

  def handleS3FileUploadRequest: Action[AnyContent] = Action { implicit req =>
    form.bindFromRequest().fold(
        _ =>
        SeeOther("/upscan-success").withHeaders("Location" -> "upscan-success"),
      stuff => {
        callBack(stuff.successActionRedirect.split("/").last)
        SeeOther(stuff.successActionRedirect)
      }
    )
  }

  def callBack(ref: String)(implicit hc: HeaderCarrier) = {
    Thread.sleep(1000)

    val notification =
    <Root>
        <FileReference>{ref}</FileReference>
        <BatchId>5e634e09-77f6-4ff1-b92a-8a9676c715c4</BatchId>
        <FileName>File{fileRef}.pdf</FileName>
        <Outcome>SUCCESS</Outcome>
        <Details>[detail block]</Details>
      </Root>

    fileRef += 1
    notificationService.save(notification)
  }
}

object XmlHelper {

  def toXml(field: (String, String)): Elem =
      <a/>.copy(label = field._1, child = Seq(Text(field._2)))

  def toXml(uploadRequest: UploadRequest): Elem =
    <UploadRequest>
      <Href>
        {uploadRequest.href}
      </Href>
      <Fields>
        {uploadRequest.fields.map(toXml)}
      </Fields>
    </UploadRequest>

  def toXml(upload: FileUpload): Elem = {
    val request = upload.state match {
      case Waiting(req) => toXml(req)
      case _ => NodeSeq.Empty
    }
    <File>
      <Reference>
        {upload.reference}
      </Reference>{request}
    </File>
  }

  def toXml(response: FileUploadResponse): Elem = {
    <FileUploadResponse xmlns="hmrc:fileupload">
      <Files>
        {response.uploads.map(toXml)}
      </Files>
    </FileUploadResponse>
  }

}
