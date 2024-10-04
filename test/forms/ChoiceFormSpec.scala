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

package forms

import base.BaseSpec
import forms.ChoiceForm.{AllowedChoiceValues, ChoiceKey}

class ChoiceFormSpec extends BaseSpec {

  "ChoiceForm" should {

    "attach error to form" when {

      "provided with empty input" in {
        val input = Map(ChoiceKey -> "")
        val result = ChoiceForm.form.bind(input)

        result.hasErrors mustBe true
        result.errors.length mustBe 1
        result.errors.head.message mustBe "choicePage.input.error.empty"
      }

      "provided with incorrect input" in {
        val input = Map(ChoiceKey -> "IncorrectChoice")
        val result = ChoiceForm.form.bind(input)

        result.hasErrors mustBe true
        result.errors.length mustBe 1
        result.errors.head.message mustBe "choicePage.input.error.incorrect"
      }
    }

    "not attach any error to form" when {
      "provided with correct input" in {
        val input = Map(ChoiceKey -> AllowedChoiceValues.DocumentUpload)
        val result = ChoiceForm.form.bind(input)

        result.hasErrors mustBe false
      }
    }
  }
}
