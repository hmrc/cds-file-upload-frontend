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
import forms.ChoiceForm
import org.jsoup.nodes.Document
import views.html.choice_page

class ChoicePageSpec extends UnitViewSpec {

  private val form = ChoiceForm.form
  private val choicePage = instanceOf[choice_page]

  "Choice page" when {
    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("choicePage.heading")
    }

    "not display the 'Back' link" in {
      assertBackLinkIsNotIncluded(view)
    }

    "display link to upload documents" in {
      val link = view.getElementsByAttributeValue("id", "upload-files").get(0)
      link must haveHref(controllers.routes.MrnEntryController.onPageLoad())
    }

    "display link to view messages" in {
      val link = view.getElementsByAttributeValue("id", "view-messages").get(0)
      link must haveHref(controllers.routes.InboxChoiceController.onPageLoad)
    }
  }

  private val view: Document = choicePage(form)(request, messages)
}
