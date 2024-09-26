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
import forms.FileUploadCountProvider
import generators.Generators
import models.FileUploadCount
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import play.twirl.api.HtmlFormat.Appendable
import views.html.how_many_files_upload

class HowManyFilesUploadSpec extends UnitViewSpec with Generators with ScalaCheckPropertyChecks {

  val mrn = arbitraryMrn.arbitrary.sample.get

  val form = new FileUploadCountProvider()()
  val page = instanceOf[how_many_files_upload]

  def createView(form: Form[FileUploadCount] = form): Document =
    asDocument(page(form, mrn)(request, messages))

  val messagePrefix = "howManyFilesUpload"

  "How Many Files Upload Page" must {

    "have the page's title prefixed with 'Error:'" when {
      "the page has errors" in {
        val view = createView(form.withGlobalError("error.summary.title"))
        view.head.getElementsByTag("title").first.text must startWith("Error: ")
      }
    }

    behave like normalPage(() => createView(), messagePrefix)

    def createAppendable(form: Form[FileUploadCount]): Appendable =
      page(form, mrn)(request, messages)

    behave like intPage(createAppendable, (form, i) => form.bind(Map("value" -> i.toString)), "value", messagePrefix)

    "display the correct guidance" in {
      val page = createView()
      val expectedGuidanceKeys: List[String] = List(
        "paragraph1",
        "paragraph2.heading",
        "paragraph2",
        "paragraph3.heading",
        "paragraph3",
        "listItem1",
        "listItem2",
        "listItem3",
        "listItem4",
        "warning"
      )
      for (key <- expectedGuidanceKeys) assertContainsText(page, messages(s"$messagePrefix.$key"))
    }

    "display inset text" in {
      assertContainsMessage(createView(), s"$messagePrefix.insetText")
    }

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page(form, mrn)(AuthenticatedRequest(request, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(createView(), routes.ContactDetailsController.onPageLoad.url)
    }
  }

  private val number = 123

  private val errorMessage = "error.number"
  private val error = FormError(key = "value", errorMessage)

  def intPage(
    createView: Form[FileUploadCount] => HtmlFormat.Appendable,
    fillForm: (Form[FileUploadCount], Int) => Form[FileUploadCount],
    fieldName: String,
    messageKeyPrefix: String
  ): Unit =
    "behave like a page with an integer value field" when {
      "rendered" must {

        "contain a label for the value" in {
          val doc = asDocument(createView(form))
          assertContainsLabel(doc, fieldName, messages(s"$messageKeyPrefix.heading"))
        }

        "contain an input for the value" in {
          val doc = asDocument(createView(form))
          assertRenderedById(doc, fieldName)
        }
      }

      "rendered with a valid form" must {
        "include the form's value in the value input" in {
          val doc = asDocument(createView(fillForm(form, number)))
          doc.getElementById("value").attr("value") mustBe number.toString
        }
      }

      "rendered with an error" must {

        "show an error summary" in {
          val doc = asDocument(createView(form.withError(error)))
          assertRenderedByClass(doc, "govuk-error-summary")
        }

        "show an error in the value field's label" in {
          val doc = asDocument(createView(form.withError(error)))
          val errorSpan = doc.getElementsByClass("govuk-error-message").first
          errorSpan.text mustBe s"Error: ${messages(errorMessage)}"
        }
      }
    }
}
