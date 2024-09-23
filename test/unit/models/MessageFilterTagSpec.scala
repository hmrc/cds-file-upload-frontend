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

package models

import base.SpecBase

class MessageFilterTagSpec extends SpecBase {

  "MessageFilterTag valueOf" should {
    "return the ExportMessages object when passed the correct string value representation" in {
      MessageFilterTag.valueOf("ExportMessages") mustBe Some(ExportMessages)
    }

    "return the ImportMessages object when passed the correct string value representation" in {
      MessageFilterTag.valueOf("ImportMessages") mustBe Some(ImportMessages)
    }

    "return nothing when passed an incorrect string value representation" in {
      MessageFilterTag.valueOf("Imports") mustBe None
    }
  }
}
