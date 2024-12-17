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

import play.api.libs.json.{Format, Json, OFormat}
import repositories.ZonedDateTimeFormat.{zonedDateTimeReads, zonedDateTimeWrites}

import java.time.{ZoneOffset, ZonedDateTime}

case class FileUploadAnswers(
  eori: String,
  uuid: String,
  mrn: Option[MRN] = None,
  contactDetails: Option[ContactDetails] = None,
  fileUploadCount: Option[FileUploadCount] = None,
  fileUploadResponse: Option[FileUploadResponse] = None,
  updated: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
  mrnPageRefererUrl: Option[String] = None
)

object FileUploadAnswers {

  implicit val format: OFormat[FileUploadAnswers] = {
    implicit val zonedDateTimeFormat: Format[ZonedDateTime] = Format(zonedDateTimeReads, zonedDateTimeWrites)

    OFormat[FileUploadAnswers](Json.reads[FileUploadAnswers], Json.writes[FileUploadAnswers])
  }
}
