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

import play.api.libs.json._

sealed trait MessageFilterTag {
  val filterValue: String
}

case object ExportMessages extends MessageFilterTag { val filterValue = "CDS-EXPORTS" }
case object ImportMessages extends MessageFilterTag { val filterValue = "CDS-IMPORTS" }

object MessageFilterTag {

  val values = Seq(ExportMessages, ImportMessages)

  def valueOf(name: String): Option[MessageFilterTag] =
    values.foldLeft(Option.empty[MessageFilterTag]) { (acc, tag) =>
      if (tag.toString.equalsIgnoreCase(name))
        Some(tag)
      else
        acc
    }

  implicit val reads: Reads[MessageFilterTag] =
    __.read[String]
      .map(valueOf(_))
      .collect(JsonValidationError("MessageFilterTag did not pass validation")) { case Some(tag) =>
        tag
      }

  implicit val writesTag: Writes[MessageFilterTag] = Writes { case value: MessageFilterTag =>
    JsString(value.toString)
  }
}
