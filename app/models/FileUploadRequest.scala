/*
 * Copyright 2018 HM Revenue & Customs
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

sealed abstract case class FileUploadRequest(
  declarationId: String,
  fileGroupSize: Int,
  files: Seq[FileUploadFile]) {

  def toXml: Elem =
    <FileUploadRequest xmlns="hmrc:fileupload">
      <DeclarationID>{declarationId}</DeclarationID>
      <FileGroupSize>{fileGroupSize}</FileGroupSize>
      <Files>{files.map(_.toXml)}</Files>
    </FileUploadRequest>
}

object FileUploadRequest {

  def apply(
    declarationId: String,
    fileGroupSize: Int,
    files: Seq[FileUploadFile]): Option[FileUploadRequest] = {

    if (declarationId.length > 0 && declarationId.length < 23 && fileGroupSize > 0 && files.nonEmpty) {
      Some(new FileUploadRequest(declarationId, fileGroupSize, files) {})
    } else {
      None
    }
  }
}

sealed abstract case class FileUploadFile(
  fileSequenceNo: Int,
  documentType: String) {

  def toXml: Elem =
    <File>
      <FileSequenceNo>{fileSequenceNo}</FileSequenceNo>
      <DocumentType>{documentType}</DocumentType>
    </File>
}

object FileUploadFile {

  def apply(fileSequenceNo: Int, documentType: String): Option[FileUploadFile] =
    if (fileSequenceNo > 0) Some(new FileUploadFile(fileSequenceNo, documentType) {})
    else None
}