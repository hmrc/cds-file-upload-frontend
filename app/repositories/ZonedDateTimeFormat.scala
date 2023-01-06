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

package repositories

import play.api.libs.json._

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import scala.util.Try

object ZonedDateTimeFormat {

  lazy val zonedDateTimeReads: Reads[ZonedDateTime] =
    fromDateObj.orElse(fromNumberLongObj).orElse(fromDateTimeStringWithZone)

  val zonedDateTimeWrites: Writes[ZonedDateTime] =
    instantWrites.contramap(res => res.withZoneSameInstant(ZoneId.of("UTC")).toInstant)

  lazy val zonedDateTimeFormat: Format[ZonedDateTime] = Format(zonedDateTimeReads, zonedDateTimeWrites)

  private val fromNumberLongObj: Reads[ZonedDateTime] =
    Reads.at[String](__ \ "$date" \ "$numberLong").map(s => Instant.ofEpochMilli(s.toLong).atZone(ZoneOffset.UTC))

  private val fromDateObj: Reads[ZonedDateTime] =
    Reads.at[Long](__ \ "$date").map(Instant.ofEpochMilli(_).atZone(ZoneOffset.UTC))

  private val fromDateTimeStringWithZone: Reads[ZonedDateTime] =
    (value: JsValue) =>
      Try {
        JsSuccess(ZonedDateTime.parse(value.as[String]))
      }.getOrElse(JsError(s"[$value] cannot be parsed to ZonedDateTime"))

  lazy val instantWrites: Writes[Instant] = Writes.at[Long](__ \ "$date").contramap(_.toEpochMilli)
}

case class JsZonedDateTime(value: ZonedDateTime)
