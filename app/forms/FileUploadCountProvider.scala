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

package forms

import com.google.inject.Inject
import forms.mappings.Mappings
import models.FileUploadCount
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

import scala.util.Try

class FileUploadCountProvider @Inject extends Mappings {

  def apply(): Form[FileUploadCount] = Form("value" -> of(fileUploadCountFormatter("howManyFilesUpload.invalid")))

  def fileUploadCountFormatter(errorKey: String): Formatter[FileUploadCount] = new Formatter[FileUploadCount] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], FileUploadCount] =
      data.get(key)
        .flatMap(s => Try(s.toInt).toOption)
        .flatMap(FileUploadCount(_)) match {
          case None        => Left(Seq(FormError(key, errorKey)))
          case Some(count) => Right(count)
        }

    override def unbind(key: String, value: FileUploadCount): Map[String, String] =
      Map(key -> value.value.toString)
  }

}
