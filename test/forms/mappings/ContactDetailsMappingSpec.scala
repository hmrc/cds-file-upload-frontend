/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form

class ContactDetailsMappingSpec extends SpecBase with MustMatchers with ScalaCheckPropertyChecks with Generators {

  val form = Form(contactDetailsMapping)

  val errorMessage: Form[_] => String = _.errors.map(_.message).headOption.getOrElse("")

  "contactDetailsMapping" should {

    "bind" when {

      "valid values are bound" in {

        forAll { contactDetails: ContactDetails =>
          Form(contactDetailsMapping).fillAndValidate(contactDetails).fold(_ => fail("form should not have errors"), _ mustBe contactDetails)
        }
      }
    }

    "fail" when {

      "name is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) { (contactDetails, invalidName) =>
          val badData = contactDetails.copy(name = invalidName)

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.name.invalid", _ => fail("form should not succeed"))
        }
      }

      "Company name is larger than 35 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(36)) { (contactDetails, invalidCompanyName) =>
          val badData = contactDetails.copy(companyName = invalidCompanyName)

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.companyName.invalid", _ => fail("form should not succeed"))
        }
      }

      "Phone number is invalid" in {

        forAll(arbitrary[ContactDetails], alphaNumString()) { (contactDetails, invalidPhoneNumber) =>
          val badData = contactDetails.copy(phoneNumber = invalidPhoneNumber.take(15))

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.invalidPattern", _ => fail("form should not succeed"))
        }
      }

      "Phone number is larger than 15 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(16)) { (contactDetails, invalidPhoneNumber) =>
          val badData = contactDetails.copy(phoneNumber = invalidPhoneNumber)

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.phoneNumber.invalid", _ => fail("form should not succeed"))
        }
      }

      "Email is invalid" in {

        forAll(arbitrary[ContactDetails], stringsWithMaxLength(50)) { (contactDetails, invalidEmail) =>
          val badData = contactDetails.copy(email = invalidEmail)

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.email.invalidPattern", _ => fail("form should not succeed"))
        }
      }

      "Email is larger than 50 chars" in {

        forAll(arbitrary[ContactDetails], minStringLength(51)) { (contactDetails, invalidEmail) =>
          val badData = contactDetails.copy(email = invalidEmail)

          Form(contactDetailsMapping)
            .fillAndValidate(badData)
            .fold(errors => errorMessage(errors) mustBe "contactDetails.email.invalid", _ => fail("form should not succeed"))
        }
      }
    }
  }
}
