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

import base.UnitSpec
import org.scalacheck.Gen
import play.api.data.Form

class FileUploadCountProviderSpec extends UnitSpec {

  private val validFileUploadCountGen = Gen.chooseNum(1, 10)
  private val form = new FileUploadCountProvider()()
  private val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "formProvider" should {

    "return success for valid FileUploadCount values" in {
      forAll(validFileUploadCountGen) { count: Int =>
        form.bind(Map("value" -> count.toString)).fold(_ => fail("Form binding must not fail!"), result => result.value mustBe count)
      }
    }

    "return invalid error for invalid FileUploadCount values" in {
      forAll { count: Int =>
        if (count < 1 || count > 10) {
          form
            .bind(Map("value" -> count.toString))
            .fold(errors => errorMessage(errors) mustBe "howManyFilesUpload.invalid", _ => fail("Form binding must fail!"))
        }
      }
    }

    "return missing error for an empty or only whitespace FileUploadCount value" in {
      form
        .bind(Map.empty[String, String])
        .fold(errors => errorMessage(errors) mustBe "howManyFilesUpload.missing", _ => fail("Form binding must not fail!"))
      form
        .bind(Map("value" -> "  "))
        .fold(errors => errorMessage(errors) mustBe "howManyFilesUpload.missing", _ => fail("Form binding must not fail!"))
    }
  }
}
