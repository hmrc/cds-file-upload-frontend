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

import forms.ChoiceForm
import forms.ChoiceForm.AllowedChoiceValues
import org.jsoup.nodes.Document
import views.html.choice_page
import views.matchers.ViewMatchers

class ChoicePageSpec extends DomAssertions with ViewMatchers {

  private val choicePage = instanceOf[choice_page]
  private val form = ChoiceForm.form

  private val view: Document = choicePage(form)(fakeRequest, messages)

  "Choice page" should {

    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("choicePage.heading")
    }

    "not display the 'Back' link" in {
      assertBackLinkIsNotIncluded(view)
    }

    "display radio buttons" in {
      view must containElementWithID(AllowedChoiceValues.SecureMessageInbox)
      view must containElementWithID(AllowedChoiceValues.DocumentUpload)
    }

    "display 'Continue' button" in {
      view.getElementsByClass("govuk-button").first() must containMessage("common.continue")
    }
  }
}
