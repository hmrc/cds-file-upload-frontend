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

package forms.mappings

import base.SpecBase
import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models.ContactDetails
import org.scalacheck.Arbitrary._
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form

class ContactDetailsMappingSpec extends SpecBase with Matchers with ScalaCheckPropertyChecks with Generators {

  val form = Form(contactDetailsMapping)

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")
  val validContactDetails = ContactDetails("name", "companyName", "0123456789")

  "contactDetailsMapping" should {

    "bind" when {

      "valid values are bound" in {
        forAll { contactDetails: ContactDetails =>
          form
            .fillAndValidate(contactDetails)
            .fold(_ => fail("form should not have errors"), _ mustBe contactDetails)
        }
      }
    }

    "fail" when {

      "name value" that {
        "is larger than 35 chars" in {
          forAll(arbitrary[ContactDetails], minStringLength(36)) { (contactDetails, invalidName) =>
            val badData = contactDetails.copy(name = invalidName)

            form
              .fillAndValidate(badData)
              .fold(errors => errorMessage(errors) mustBe "contactDetails.name.invalid", _ => fail("form should not succeed"))
          }
        }

        "is empty" in {
          val formData = contactDetailsMapping.unbind(validContactDetails).filterKeys(_ != "name")
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.name.missing", _ => fail("Form binding must not fail!"))
        }

        "is just whitespace chars" in {
          val formData = contactDetailsMapping.unbind(validContactDetails.copy(name = "   "))
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.name.missing", _ => fail("Form binding must not fail!"))
        }
      }

      "Company name" that {
        "is larger than 35 chars" in {
          forAll(arbitrary[ContactDetails], minStringLength(36)) { (contactDetails, invalidCompanyName) =>
            val badData = contactDetails.copy(companyName = invalidCompanyName)

            form
              .fillAndValidate(badData)
              .fold(errors => errorMessage(errors) mustBe "contactDetails.companyName.invalid", _ => fail("form should not succeed"))
          }
        }

        "is empty" in {
          val formData = contactDetailsMapping.unbind(validContactDetails).filterKeys(_ != "companyName")
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.companyName.missing", _ => fail("Form binding must not fail!"))
        }

        "is just whitespace chars" in {
          val formData = contactDetailsMapping.unbind(validContactDetails.copy(companyName = "   "))
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.companyName.missing", _ => fail("Form binding must not fail!"))
        }
      }

      "Phone number" that {
        "is invalid" in {
          forAll(arbitrary[ContactDetails], alphaString()) { (contactDetails, invalidPhoneNumber) =>
            val badData = contactDetails.copy(phoneNumber = invalidPhoneNumber.take(15))

            form
              .fillAndValidate(badData)
              .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.invalidPattern", _ => fail("form should not succeed"))
          }
        }

        "is larger than 15 chars" in {
          forAll(arbitrary[ContactDetails], minStringLength(16)) { (contactDetails, invalidPhoneNumber) =>
            val badData = contactDetails.copy(phoneNumber = invalidPhoneNumber)

            form
              .fillAndValidate(badData)
              .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.invalid", _ => fail("form should not succeed"))
          }
        }

        "is empty" in {
          val formData = contactDetailsMapping.unbind(validContactDetails).filterKeys(_ != "phoneNumber")
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.missing", _ => fail("Form binding must not fail!"))
        }

        "is just whitespace chars" in {
          val formData = contactDetailsMapping.unbind(validContactDetails.copy(phoneNumber = "   "))
          form
            .bind(formData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.missing", _ => fail("Form binding must not fail!"))
        }
      }
    }
  }
}
