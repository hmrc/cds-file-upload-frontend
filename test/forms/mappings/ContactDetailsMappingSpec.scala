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

package forms.mappings

import base.SpecBase
import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models.ContactDetails
import org.scalacheck.Arbitrary._
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import play.api.data.Form

class ContactDetailsMappingSpec extends SpecBase
  with MustMatchers
  with PropertyChecks
  with Generators {

  val form = Form(contactDetailsMapping)

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "contactDetailsMapping" should {

    "bind" when {

      "valid values are bound" in {

        forAll { contactDetails: ContactDetails =>

          Form(contactDetailsMapping).fillAndValidate(contactDetails).fold(
            e => fail("form should not have errors"),
            _ mustBe contactDetails
          )
        }
      }
    }

    "fail" when {

      "name is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) {
          (contactDetails, invalidName) =>
            val badData = contactDetails.copy(name = invalidName)

            Form(contactDetailsMapping).fillAndValidate(badData).fold(
              errors => errorMessage(errors) mustBe "Name must be less than or equal to 35 characters",
              _ => fail("form should not succeed")
            )
        }
      }

      "Company name is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) {
          (contactDetails, invalidCompanyName) =>
            val badData = contactDetails.copy(companyName = invalidCompanyName)

            Form(contactDetailsMapping).fillAndValidate(badData).fold(
              errors => errorMessage(errors) mustBe "Company name must be less than or equal to 35 characters",
              _ => fail("form should not succeed")
            )
        }
      }

      "Phone number is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) {
          (contactDetails, invalidPhoneNumber) =>
            val badData = contactDetails.copy(phoneNumber = invalidPhoneNumber)

            Form(contactDetailsMapping).fillAndValidate(badData).fold(
              errors => errorMessage(errors) mustBe "Phone number must be less than or equal to 35 characters",
              _ => fail("form should not succeed")
            )
        }
      }

      "Email is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) {
          (contactDetails, invalidEmail) =>
            val badData = contactDetails.copy(email = invalidEmail)

            Form(contactDetailsMapping).fillAndValidate(badData).fold(
              errors => errorMessage(errors) mustBe "Email must be less than or equal to 35 characters",
              _ => fail("form should not succeed")
            )
        }
      }
    }
  }
}
