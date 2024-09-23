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

class FileUploadRequestSpec extends SpecBase with XmlBehaviours {

  ".toXml" should {
    "pass schema validation" in {

      forAll { request: FileUploadRequest =>
        validateFileUploadRequest(request.toXml) mustBe true
      }
    }
  }

  "FileUploadFile.apply" should {

    "return Some" when {

      "all arguments are valid" in {
        FileUploadFile(2, "my doc type", "") mustBe defined

      }
    }

    "contain the redirect endpoints" in {
      FileUploadFile(2, "my doc type", "").get.successRedirect.contains("/cds-file-upload-service/upload/upscan-success/") mustBe true
      FileUploadFile(2, "my doc type", "").get.errorRedirect.contains("/cds-file-upload-service/upload/upscan-error/") mustBe true

    }

    "return None" when {

      "sequence number is less than 1" in {
        FileUploadFile(0, "some doc type", "") mustBe empty
      }
    }
  }

}
