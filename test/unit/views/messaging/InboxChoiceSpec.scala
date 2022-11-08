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

package views.messaging

import controllers.routes
import forms.InboxChoiceForm
import forms.InboxChoiceForm.{InboxChoiceKey, Values}
import forms.InboxChoiceForm.Values.ExportsMessages
import org.jsoup.nodes.Document
import views.DomAssertions
import views.html.messaging.inbox_choice
import views.matchers.ViewMatchers

class InboxChoiceSpec extends DomAssertions with ViewMatchers {

  private val inboxChoice = instanceOf[inbox_choice]
  private val form = InboxChoiceForm.form

  private val view: Document = inboxChoice(form)(fakeRequest, messages)

  "The Message Inbox Choice page" when {
    "form does not contain errors" should {
      commonAssertions()
    }

    "form contains errors" should {
      commonAssertions()

      val errorView = inboxChoice(form.withError(InboxChoiceKey, "choicePage.input.error.empty"))(fakeRequest, messages)

      "display error box at top of page" in {
        errorView.getElementsByClass("govuk-error-summary__title").first() must containMessage("error.summary.title")
      }

      "display error box with a link to first radio option" in {
        val errorLink = errorView.getElementsByClass("govuk-list govuk-error-summary__list").first().getElementsByTag("a").first()

        errorLink must containMessage("choicePage.input.error.empty")
        errorLink must haveHref(s"#${ExportsMessages}")
      }
    }

  }

  private def commonAssertions() = {
    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("inboxChoice.heading")
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(view, routes.ChoiceController.onPageLoad.url)
    }

    "display radio buttons" in {
      view must containElementWithID(Values.ExportsMessages)
      view must containElementWithID(Values.ImportsMessages)
    }

    "display 'Continue' button" in {
      view.getElementsByClass("govuk-button").first() must containMessage("common.continue")
    }
  }
}
