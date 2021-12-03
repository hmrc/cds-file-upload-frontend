/*
 * Copyright 2021 HM Revenue & Customs
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

package repositories

import play.api.libs.json.{Format, JsError, JsValue, Reads, Writes, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantWrites

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.util.Try

object ZonedDateTimeFormat {

  final val instantReads: Reads[Instant] = Reads.at[Long](__ \ "$date") map Instant.ofEpochMilli

  val zonedDateTimeReads: Reads[ZonedDateTime] = (json: JsValue) => {
    Try(json.validate[ZonedDateTime] {
      instantReads.map(ZonedDateTime.ofInstant(_, ZoneId.of("UTC")))
    })
    .getOrElse(JsError())
  }

  val zonedDateTimeWrites: Writes[ZonedDateTime] =
    instantWrites.contramap(_.withZoneSameInstant(ZoneId.of("UTC")).toInstant)

  lazy val zonedDateTimeFormat: Format[ZonedDateTime] = Format(zonedDateTimeReads, zonedDateTimeWrites)
}
