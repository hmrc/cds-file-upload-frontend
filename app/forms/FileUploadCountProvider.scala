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

package forms

import models.FileUploadCount
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

import scala.util.Try

class FileUploadCountProvider {

  def apply(): Form[FileUploadCount] = Form("value" -> of(fileUploadCountFormatter()))

  private def isMissing(key: String, data: Map[String, String]) =
    data.get(key).map(_.trim) match {
      case None | Some("") => Left(Seq(FormError(key, "howManyFilesUpload.missing")))
      case Some(count)     => Right(count)
    }

  private def isInvalid(key: String, data: String) =
    Try(data.toInt).toOption
      .flatMap(FileUploadCount(_)) match {
      case None        => Left(Seq(FormError(key, "howManyFilesUpload.invalid")))
      case Some(count) => Right(count)
    }

  def fileUploadCountFormatter(): Formatter[FileUploadCount] = new Formatter[FileUploadCount] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], FileUploadCount] =
      for {
        value <- isMissing(key, data)
        count <- isInvalid(key, value)
      } yield count

    override def unbind(key: String, value: FileUploadCount): Map[String, String] =
      Map(key -> value.value.toString)
  }
}
