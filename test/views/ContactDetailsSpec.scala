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

package views

import base.UnitViewSpec
import controllers.routes
import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models.ContactDetails
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import views.html.components.gds.exportsInputText
import views.html.contact_details

class ContactDetailsSpec extends UnitViewSpec with ScalaCheckPropertyChecks with Generators {

  val mrn = arbitraryMrn.arbitrary.sample.get

  val form = Form(contactDetailsMapping)
  val contactDetails = instanceOf[contact_details]

  val exportsInputText = instanceOf[exportsInputText]

  def createView(form: Form[ContactDetails]) = asDocument(contactDetails(form, mrn)(messages, request))

  "Contact details page" must {

    "have the page's title prefixed with 'Error:'" when {
      "the page has errors" in {
        val view = createView(form.withGlobalError("error.summary.title"))
        view.head.getElementsByTag("title").first.text must startWith("Error: ")
      }
    }

    behave like normalPage(() => createView(form), "contactDetails")

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = contactDetails(form, mrn)(messages, AuthenticatedRequest(request, user))
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(createView(form), routes.MrnEntryController.onPageLoad().url)
    }

    def createViewAsString(form: Form[ContactDetails]): String =
      contactDetails(form, mrn)(messages, request).toString

    "display name input" in {
      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("name"), labelKey = "contactDetails.name", labelClasses = "govuk-label")

        createViewAsString(popForm) must include(input.toString())
      }
    }

    "display company name input" in {
      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(field = popForm("companyName"), labelKey = "contactDetails.companyName", labelClasses = "govuk-label")

        createViewAsString(popForm) must include(input.toString())
      }
    }

    "display phone number input" in {
      forAll { contactDetails: ContactDetails =>
        val popForm = form.fillAndValidate(contactDetails)
        val input = exportsInputText(
          field = popForm("phoneNumber"),
          labelKey = "contactDetails.phoneNumber",
          labelClasses = "govuk-label",
          inputmode = "numeric",
          pattern = "[0-9]*"
        )

        createViewAsString(popForm) must include(input.toString())
      }
    }
  }
}
