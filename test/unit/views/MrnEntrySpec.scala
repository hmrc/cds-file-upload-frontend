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

import forms.MRNFormProvider
import models.MRN
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.twirl.api.HtmlFormat.Appendable
import utils.FakeRequestCSRFSupport._
import views.behaviours.StringViewBehaviours
import views.html.mrn_entry

class MrnEntrySpec extends DomAssertions with StringViewBehaviours[MRN] with ScalaCheckPropertyChecks {

  val testBackLink = "testBackLink"
  val messagePrefix = "mrnEntryPage"

  val form = new MRNFormProvider()()
  val page = instanceOf[mrn_entry]

  def createAppendable(form: Form[MRN]): Appendable =
    page(form, testBackLink)(fakeRequest.withCSRFToken, messages)

  def createView(form: Form[MRN] = form): Document = asDocument(createAppendable(form))

  "MRN Entry Page" must {

    "have the page's title prefixed with 'Error:'" when {
      "the page has errors" in {
        val view = createView(form.withGlobalError("error.summary.title"))
        view.head.getElementsByTag("title").first.text must startWith("Error: ")
      }
    }

    behave like normalPage(() => createView(), messagePrefix)

    behave like stringPage(createAppendable, "value", messagePrefix)

    "include a paragraph" in {
      createView().getElementsByClass("govuk-body").first.text.contains(messages("mrnEntryPage.paragraph"))
    }

    "include a hidden label" in {
      createView().getElementsByTag("label").first.text.contains(messages("mrnEntryPage.label"))
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
