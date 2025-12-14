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
import forms.MRNFormProvider
import generators.Generators
import models.MRN
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import play.twirl.api.HtmlFormat.Appendable
import utils.FakeRequestCSRFSupport._
import views.html.mrn_entry

class MrnEntrySpec extends UnitViewSpec with Generators with ScalaCheckPropertyChecks {

  val messagePrefix = "mrnEntryPage"

  val form = new MRNFormProvider()()
  val page = instanceOf[mrn_entry]

  def createAppendable(form: Form[MRN]): Appendable =
    page(form)(request.withCSRFToken, messages)

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
      forAll { (user: SignedInUser) =>
        val authenticatedRequest = AuthenticatedRequest(request.withCSRFToken, user)
        val view = page(form)(authenticatedRequest, messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link with URL provided" in {
      val view = page(form)(request.withCSRFToken, messages)
      assertBackLinkIsIncluded(asDocument(view))
    }
  }

  private val answer = "answer"

  private val errorMessage = "error.number"
  private val error = FormError(key = "value", errorMessage)

  def stringPage(
    createView: Form[MRN] => HtmlFormat.Appendable,
    fieldName: String,
    messageKeyPrefix: String,
    expectedHintKey: List[String] = List()
  ): Unit =
    s"behave like a page with a string value field of '$fieldName'" when {
      "rendered" must {

        "contain a label for the value" in {
          val doc = asDocument(createView(form))
          val expectedHintText = expectedHintKey map (k => messages(k))
          assertContainsLabel(doc, fieldName, messages(s"$messageKeyPrefix.heading"), expectedHintText)
        }

        "contain an input for the value" in {
          val doc = asDocument(createView(form))
          assertRenderedById(doc, fieldName)
        }
      }

      "rendered with a valid form" must {
        "include the form's value in the value input" in {
          val boundForm = form.bind(Map(fieldName -> answer))
          val doc = asDocument(createView(boundForm))

          doc.getElementById(fieldName).`val`() mustBe answer
        }
      }

      "rendered with an error" must {

        "show an error summary" in {
          val doc = asDocument(createView(form.withError(error)))
          assertRenderedByClass(doc, "govuk-error-summary")
        }

        "show an error in the value field's label" in {
          val doc = asDocument(createView(form.withError(FormError(fieldName, errorMessage))))
          val errorSpan = doc.getElementsByClass("govuk-error-message").first
          errorSpan.text mustBe s"Error: ${messages(errorMessage)}"
        }
      }
    }
}
