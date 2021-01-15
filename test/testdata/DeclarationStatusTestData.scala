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

package testdata

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import models.dis.{DeclarationStatus, PreviousDocument}
import testdata.CommonTestData._

object DeclarationStatusTestData {

  val zoneUTC = ZoneId.of("UTC")

  val declarationStatus = DeclarationStatus(
    mrn = mrn,
    versionId = "1",
    eori = eori,
    declarationType = "IMZ",
    ucr = Some(ucr),
    receivedDateTime = ZonedDateTime.of(LocalDateTime.of(2019, 7, 2, 11, 7, 57), zoneUTC),
    releasedDateTime = Some(ZonedDateTime.of(LocalDateTime.of(2019, 7, 2, 11, 7, 57), zoneUTC)),
    acceptanceDateTime = Some(ZonedDateTime.of(LocalDateTime.of(2019, 7, 2, 11, 7, 57), zoneUTC)),
    createdDateTime = ZonedDateTime.of(LocalDateTime.of(2020, 3, 10, 1, 13, 57), zoneUTC),
    roe = "6",
    ics = "15",
    irc = Some("000"),
    totalPackageQuantity = "10",
    goodsItemQuantity = "100",
    previousDocuments = Seq(
      PreviousDocument("18GBAKZ81EQJ2FGVR", "DCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVA", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVB", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVC", "DCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVD", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVE", "MCR")
    )
  )

}
