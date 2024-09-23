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

import play.api.libs.json._

case class MRN(value: String)

object MRN {

  def validRegex: String = "\\d{2}[a-zA-Z]{2}[a-zA-Z0-9]{14}"

  def apply(value: String): Option[MRN] = if (value.matches(validRegex)) Some(new MRN(value) {}) else None

  implicit val reads: Reads[MRN] =
    __.read[String]
      .map(MRN(_))
      .collect(JsonValidationError("MRN did not pass validation")) { case Some(mrn) =>
        mrn
      }

  implicit val writes: Writes[MRN] = Writes { case MRN(value) =>
    JsString(value)
  }
}
