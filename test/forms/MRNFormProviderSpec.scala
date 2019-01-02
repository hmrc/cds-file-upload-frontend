/*
 * Copyright 2019 HM Revenue & Customs
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

package forms

import base.SpecBase
import models.MRN
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import wolfendale.scalacheck.regexp.RegexpGen

class MRNFormProviderSpec extends SpecBase with PropertyChecks {

  val validMRNGen: Gen[String] = RegexpGen.from(MRN.validRegex)
  val form: Form[MRN] = new MRNFormProvider()()

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "formProvider" should {

    "return success for valid MRN" in {

      forAll(validMRNGen) { mrn =>

        form.bind(Map("value" -> mrn)).fold(
          _      => fail("Form binding must not fail!"),
          result => result.value mustBe mrn
        )
      }
    }

    "return invalid error for invalid MRN" in {

      forAll { mrn: String =>
        whenever(!mrn.matches(MRN.validRegex)) {

          form.bind(Map("value" -> mrn)).fold(
            errors => errorMessage(errors) mustBe "mrn.invalid",
            _      => fail("Form binding must fail!")
          )
        }
      }
    }
  }
}