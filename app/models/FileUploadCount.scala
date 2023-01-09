/*
 * Copyright 2023 HM Revenue & Customs
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

//import play.api.libs.json.JsonValidationError
import play.api.libs.json._

sealed abstract case class FileUploadCount(value: Int)

object FileUploadCount {

  val maxNumberOfFiles: Int = 10

  def apply(value: Int): Option[FileUploadCount] =
    if (value > 0 && value <= maxNumberOfFiles) Some(new FileUploadCount(value) {})
    else None

  implicit val reads: Reads[FileUploadCount] =
    __.read[Int]
      .map(FileUploadCount(_))
      .collect(JsonValidationError("FileUploadCount did not pass validation")) {
        case Some(count) => count
      }

  implicit val writes: Writes[FileUploadCount] = Writes {
    case FileUploadCount(value) => JsNumber(value)
  }
}
