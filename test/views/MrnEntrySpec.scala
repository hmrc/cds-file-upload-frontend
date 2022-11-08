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

package views

import forms.MRNFormProvider
import models.MRN
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.behaviours.StringViewBehaviours
import views.html.mrn_entry

class MrnEntrySpec extends DomAssertions with StringViewBehaviours[MRN] with ScalaCheckPropertyChecks {

  val form = new MRNFormProvider()()
  val testBackLink = "testBackLink"
  val messagePrefix = "mrnEntryPage"

  val page = instanceOf[mrn_entry]
  val view = asDocument(createViewUsingForm(form))

  def createViewUsingForm: Form[MRN] => HtmlFormat.Appendable =
    form => page(form, testBackLink)(fakeRequest.withCSRFToken, messages)

  "MRN Entry Page" must {
    behave like normalPage(() => view, messagePrefix)

    behave like stringPage(createViewUsingForm, "value", messagePrefix)

    val doc = asDocument(createViewUsingForm(form))

    "include a paragraph" in {
      doc.getElementsByClass("govuk-body").first.text.contains(messages("mrnEntryPage.paragraph"))
    }

    "include a hidden label" in {
      doc.getElementsByTag("label").first.text.contains(messages("mrnEntryPage.label"))
    }

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val authenticatedRequest = AuthenticatedRequest(fakeRequest.withCSRFToken, user)
        val view = page(form, testBackLink)(authenticatedRequest, messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link with URL provided" in {
      val view = page(form, testBackLink)(fakeRequest.withCSRFToken, messages)
      assertBackLinkIsIncluded(asDocument(view), testBackLink)
    }
  }
}
