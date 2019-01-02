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

import java.util.UUID

import controllers.{BatchFile, BatchFileUploadRequest, BatchFileUploadResponse, UploadRequest}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.mvc.{Action, MultipartFormData}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.xml.NodeSeq

@Singleton
class CustomsDeclarationsStubController @Inject()() extends FrontendController {

  val s3Form = Form(mapping(
    "success_action_redirect" -> text
  )(S3Form.apply)(S3Form.unapply))

  // for now, we will just return some random
  def handleBatchFileUploadRequest: Action[NodeSeq] = Action(parse.xml) { implicit req =>
    val bfur = BatchFileUploadRequest.fromXml(req.body.mkString)
    val resp = BatchFileUploadResponse((1 to bfur.fileGroupSize).map { i =>
      BatchFile(reference = UUID.randomUUID().toString, UploadRequest(
        href = "/cds-file-upload-service/test-only/s3-bucket",
        fields = Map(
          "X-Amz-Algorithm" -> "AWS4-HMAC-SHA256",
          "X-Amz-Expiration" -> "2018-02-09T12:35:45.297Z",
          "X-Amz-Signature" -> "xxxx",
          "key" -> "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
          "acl" -> "private",
          "X-Amz-Credential" -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
          "policy" -> "xxxxxxxx=="
        )
      ))
    }.toList)
    Ok(resp.toXml).as(ContentTypes.XML)
  }

  def handleS3FileUploadRequest: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit req =>
    s3Form.bindFromRequest().fold(
      errors => NoContent,
      success => Redirect(success.success_action_redirect)
    )
  }

}

case class S3Form(success_action_redirect: String)
