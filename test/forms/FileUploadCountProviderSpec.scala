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
import models.FileUploadCount
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.api.data.Form

class FileUploadCountProviderSpec extends SpecBase with PropertyChecks {

  val validFileUploadCountGen: Gen[Int] = Gen.chooseNum(1, 10)
  val form: Form[FileUploadCount] = new FileUploadCountProvider()()

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "formProvider" should {

    "return success for valid FileUploadCount" in {
      forAll(validFileUploadCountGen) { count: Int =>

        form.bind(Map("value" -> count.toString)).fold(
          _      => fail("Form binding must not fail!"),
          result => result.value mustBe count
        )
      }
    }

    "retun invalid error for invalid FileUploadCount" in {
      forAll{count: Int =>
        if(count<1 || count>10){
          form.bind(Map("value" -> count.toString)).fold(
            errors => errorMessage(errors) mustBe "howManyFilesUpload.invalid",
            _      => fail("Form binding must fail!")
          )
        }
      }
    }
  }
}
