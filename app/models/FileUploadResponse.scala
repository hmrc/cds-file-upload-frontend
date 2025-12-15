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

package models

import scala.xml.Elem

import play.api.Logging
import play.api.libs.json._

case class UploadRequest(href: String, fields: Map[String, String])

object UploadRequest {
  implicit val format: OFormat[UploadRequest] = Json.format[UploadRequest]
}

case class FileUpload(reference: String, state: FileState, filename: String = "", id: String)

object FileUpload {
  implicit val format: OFormat[FileUpload] = Json.format[FileUpload]
}

case class FileUploadResponse(uploads: List[FileUpload])

object FileUploadResponse extends Logging {
  implicit val format: OFormat[FileUploadResponse] = Json.format[FileUploadResponse]

  def apply(files: List[FileUpload]): FileUploadResponse = new FileUploadResponse(files.sortBy(_.reference)) {}

  def fromXml(xml: Elem): FileUploadResponse = {
    logger.info("File Upload Response " + xml)
    val files: List[FileUpload] = (xml \ "Files" \ "_").theSeq.collect { case file =>
      val reference = (file \ "Reference").text.trim
      val href = (file \ "UploadRequest" \ "Href").text.trim
      val successUrl = (file \ "UploadRequest" \ "Fields" \ "success_action_redirect").text.trim
      val fields: Map[String, String] =
        (file \ "UploadRequest" \ "Fields" \ "_").theSeq.collect {
          case field if field.label == "success-action-redirect" => "success_action_redirect" -> field.text.trim
          case field if field.label == "error-action-redirect"   => "error_action_redirect" -> field.text.trim
          case field                                             => field.label -> field.text.trim
        }.toMap

      FileUpload(reference, Waiting(UploadRequest(href, fields)), id = successUrl.split('/').last)
    }.toList

    FileUploadResponse(files)
  }
}

abstract class Field(value: String) {
  override def toString: String = value
}

object Field {
  case object ContentType extends Field("Content-Type")
  case object ACL extends Field("acl")
  case object Key extends Field("key")
  case object Policy extends Field("policy")
  case object Algorithm extends Field("x-amz-algorithm")
  case object Credentials extends Field("x-amz-credential")
  case object Date extends Field("x-amz-date")
  case object Callback extends Field("x-amz-meta-callback-url")
  case object Signature extends Field("x-amz-signature")
  case object SuccessRedirect extends Field("success_action_redirect")
  case object ErrorRedirect extends Field("error_action_redirect")

  val values: Set[Field] = Set(ContentType, ACL, Key, Policy, Algorithm, Credentials, Date, Signature, Callback)
}
