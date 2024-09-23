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

import forms.ChoiceForm
import forms.ChoiceForm.AllowedChoiceValues._
import forms.ChoiceForm.ChoiceKey
import org.jsoup.nodes.Document
import views.html.choice_page
import views.matchers.ViewMatchers

class ChoicePageSpec extends DomAssertions with ViewMatchers {

  private val form = ChoiceForm.form
  private val choicePage = instanceOf[choice_page]

  "Choice page" when {

    "form does not contain errors" should {
      commonAssertions()
    }

    "form contains errors" should {
      commonAssertions()

      val errorView = choicePage(form.withError(ChoiceKey, "choicePage.input.error.empty"))(fakeRequest, messages)

      "have the page's title prefixed with 'Error:'" in {
        errorView.head.getElementsByTag("title").first.text must startWith("Error: ")
      }

      "display error box at top of page" in {
        errorView.getElementsByClass("govuk-error-summary__title").first() must containMessage("error.summary.title")
      }

      "display error box with a link to first radio option" in {
        val errorLink = errorView.getElementsByClass("govuk-list govuk-error-summary__list").first().getElementsByTag("a").first()

        errorLink must containMessage("choicePage.input.error.empty")
        errorLink must haveHref(s"#${SecureMessageInbox}")
      }
    }
  }

  private val view: Document = choicePage(form)(fakeRequest, messages)

  private def commonAssertions() = {
    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("choicePage.heading")
    }

    "not display the 'Back' link" in {
      assertBackLinkIsNotIncluded(view)
    }

    "display radio buttons" in {
      view must containElementWithID(SecureMessageInbox)
      view must containElementWithID(DocumentUpload)
    }

    "display 'Continue' button" in {
      view.getElementsByClass("govuk-button").first() must containMessage("common.continue")
    }
  }
}
