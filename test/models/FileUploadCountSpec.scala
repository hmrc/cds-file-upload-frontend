/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalacheck.Gen

class FileUploadCountSpec extends SpecBase {

  val fileUploadNumber: Gen[Int] = Gen.chooseNum(-100, 100)

  "FileUploadCount.apply" should {

    "return Some for valid FileUploadCount" in {
      forAll(fileUploadNumber) { count =>
        if (count > 0 && count <= 10) {
          FileUploadCount(count).map(_.value) mustBe Some(count)
        }
      }
    }

    "return None for invalid FileUploadCount" in {
      forAll(fileUploadNumber) { count =>
        if (count <= 0 || count > 10) {
          FileUploadCount(count) mustBe None
        }
      }
    }
  }
}
