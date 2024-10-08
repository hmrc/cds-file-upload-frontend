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

import java.util.UUID

import scala.xml.Elem

final case class FileUploadRequest(declarationId: MRN, files: Seq[FileUploadFile]) {

  def toXml: Elem =
    <FileUploadRequest xmlns="hmrc:fileupload">
      <DeclarationID>{declarationId.value}</DeclarationID>
      <FileGroupSize>{files.length}</FileGroupSize>
      <Files>{files.map(_.toXml)}</Files>
    </FileUploadRequest>
}

case class FileUploadFile(fileSequenceNo: Int, documentType: String, serviceUrl: String) {
  private val uuid = UUID.randomUUID()
  val successRedirect = serviceUrl + "/cds-file-upload-service/upload/upscan-success/" + uuid
  val errorRedirect = serviceUrl + "/cds-file-upload-service/upload/upscan-error/" + uuid

  def toXml: Elem =
    <File>
      <FileSequenceNo>{fileSequenceNo}</FileSequenceNo>
      <DocumentType>{documentType}</DocumentType>
      <SuccessRedirect>{successRedirect}</SuccessRedirect>
      <ErrorRedirect>{errorRedirect}</ErrorRedirect>
    </File>
}

object FileUploadFile {

  def apply(fileSequenceNo: Int, documentType: String, serviceUrl: String): Option[FileUploadFile] =
    if (fileSequenceNo > 0) Some(new FileUploadFile(fileSequenceNo, documentType, serviceUrl) {})
    else None
}
