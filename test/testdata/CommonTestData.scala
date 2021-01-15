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

import models.ContactDetails

object CommonTestData {

  val mrn = "18GB9JLC3CU1LFGVR1"
  val mrn_2 = "18GB9JLC3CU1LFGVR2"
  val eori = "GB123456789012000"
  val eori_2 = "GB123456789012001"
  val ucr = "20GBAKZ81EQJ2WXYZ"

  val contactDetails = ContactDetails(name = "name", companyName = "company name", phoneNumber = "0123456789", email = "email@mail.org")
}
