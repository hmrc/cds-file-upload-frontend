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

package models

import base.SpecBase
import generators.Generators
import org.scalatest.prop.PropertyChecks

class FileUploadRequestSpec extends SpecBase with XmlBehaviours with PropertyChecks with Generators {

  ".toXml" should {
    "pass schema validation" in {

      forAll { request: FileUploadRequest =>

        validateFileUploadRequest(request.toXml) mustBe true
      }
    }
  }

  ".apply" should {

    "return Some" when {

      "all arguments are valid" in {

        forAll(fileUploadRequestGen) {
          case request@FileUploadRequest(decId, fileGroupSize, files) =>

            FileUploadRequest(decId, fileGroupSize, files) mustBe Some(request)
        }
      }
    }

    "return None" when {

      "declaration id is empty" in {

        forAll(fileUploadRequestGen) {
          case FileUploadRequest(_, fileGroupSize, files) =>

            FileUploadRequest("", fileGroupSize, files) mustBe None
        }
      }

      "declaration id is longer than 22 characters" in {

        forAll(stringsLongerThan(22), fileUploadRequestGen) {
          case (decId, FileUploadRequest(_, fileGroupSize, files)) =>

            FileUploadRequest(decId, fileGroupSize, files) mustBe None
        }
      }

      "fileGroupSize is less than 1" in {

        forAll(intsBelowValue(1), fileUploadRequestGen) {
          case (fileGroupSize, FileUploadRequest(decId, _, files)) =>

            FileUploadRequest(decId, fileGroupSize, files) mustBe None
        }
      }

      "files is empty" in {

        forAll(fileUploadRequestGen) {
          case FileUploadRequest(decId, fileGroupSize, _) =>

            FileUploadRequest(decId, fileGroupSize, Seq()) mustBe None
        }
      }
    }
  }

  "FileUploadFile.apply" should {

    "return Some" when {

      "all arguments are valid" in {

        forAll(fileUploadFileGen) {
          case file @ FileUploadFile(seqNo, docType) =>

            FileUploadFile(seqNo, docType) mustBe Some(file)
        }
      }
    }

    "return None" when {

      "sequence number is less than 1" in {

        forAll(intsBelowValue(1), fileUploadFileGen) {
          case (seqNo, FileUploadFile(_, documentType)) =>

            FileUploadFile(seqNo, documentType) mustBe None
        }
      }
    }
  }
}