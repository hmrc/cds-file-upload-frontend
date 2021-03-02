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

package views

import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models.{ContactDetails, MRN}
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.twirl.api.Html
import utils.FakeRequestCSRFSupport._
import views.behaviours.ViewBehaviours
import views.html.components.gds.exportsInputText
import views.html.contact_details

class ContactDetailsSpec extends DomAssertions with ViewBehaviours with ScalaCheckPropertyChecks with Generators {

  val form: Form[ContactDetails] = Form(contactDetailsMapping)
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  val page: contact_details = instanceOf[contact_details]
  val exportsInputText = instanceOf[exportsInputText]

  val simpleView: () => Html = () => page(form, mrn)(fakeRequest.withCSRFToken, messages)

  def view(form: Form[ContactDetails] = form): String = page(form, mrn)(fakeRequest.withCSRFToken, messages).toString()

  val messagePrefix = "contactDetails"

  def getMessage(key: String): String = messages(s"$messagePrefix.$key")

  "Contact details page" must {

    behave like normalPage(simpleView, messagePrefix)

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page(form, mrn)(AuthenticatedRequest(fakeRequest.withCSRFToken, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display name input" in {

      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("name"), labelKey = "contactDetails.name", labelClasses = "govuk-label")

        view(popForm) must include(input.toString())
      }
    }

    "display company name input" in {

      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("companyName"), labelKey = "contactDetails.companyName", labelClasses = "govuk-label")

        view(popForm) must include(input.toString())
      }
    }

    "display phone number input" in {

      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("phoneNumber"), labelKey = "contactDetails.phoneNumber", labelClasses = "govuk-label")

        view(popForm) must include(input.toString())
      }
    }

    "display email input" in {

      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("email"), labelKey = "contactDetails.email", labelClasses = "govuk-label")

        view(popForm) must include(input.toString())
      }
    }
  }
}
