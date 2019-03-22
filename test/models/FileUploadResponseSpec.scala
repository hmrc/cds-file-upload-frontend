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
import controllers.test.XmlHelper
import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest.prop.PropertyChecks

class FileUploadResponseSpec extends SpecBase with XmlBehaviours with PropertyChecks with Generators {

  val fileGen: Gen[File] = arbitrary[File].flatMap(file => arbitrary[Waiting].map(waiting => file.copy(state = waiting)))
  val responseGen: Gen[FileUploadResponse] =
    listOfN(10, fileGen).map(files => FileUploadResponse(files))

  ".fromXml" should {

    "parse all fields" in {

      forAll(responseGen) { response =>

        val xml = XmlHelper.toXml(response)
        FileUploadResponse.fromXml(xml) mustBe response
      }
    }
  }
}