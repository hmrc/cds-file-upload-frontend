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

package models

import base.SpecBase
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import testdata.CommonTestData.mrn

class MRNSpec extends SpecBase with ScalaCheckPropertyChecks {

  "MRN.apply" should {

    "return Some for valid MRN" in {

      MRN(mrn).map(_.value) mustBe Some(mrn)
    }

    "return None for invalid MRN" in {

      val invalidMrn = "12GBINVALIDMRN"
      MRN(invalidMrn) mustBe None
    }
  }
}
