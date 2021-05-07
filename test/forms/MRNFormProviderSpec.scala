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

package forms

import base.SpecBase
import play.api.data.Form
import testdata.CommonTestData.mrn

class MRNFormProviderSpec extends SpecBase {

  val form = new MRNFormProvider()()

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "formProvider" should {

    "return success for valid MRN values" in {

      form.bind(Map("value" -> mrn)).fold(_ => fail("Form binding must not fail!"), result => result.value mustBe mrn)
    }

    "return invalid error for some invalid MRN values" in {

      val invalidMrn = "12GBINVALIDMRN"
      form.bind(Map("value" -> invalidMrn)).fold(errors => errorMessage(errors) mustBe "mrn.invalid", _ => fail("Form binding must fail!"))
    }

    "return missing error for an empty or only whitespace MRN value" in {

      form.bind(Map.empty[String, String]).fold(errors => errorMessage(errors) mustBe "mrn.missing", _ => fail("Form binding must fail!"))
      form.bind(Map("value" -> "   ")).fold(errors => errorMessage(errors) mustBe "mrn.missing", _ => fail("Form binding must fail!"))
    }
  }
}
