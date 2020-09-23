/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class UserAnswers(
  eori: String,
  contactDetails: Option[ContactDetails] = None,
  mrn: Option[MRN] = None,
  fileUploadCount: Option[FileUploadCount] = None,
  fileUploadResponse: Option[FileUploadResponse] = None,
  updated: DateTime = DateTime.now.withZone(DateTimeZone.UTC)
)

object UserAnswers {
  implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val answersFormat = Json.format[UserAnswers]
}
