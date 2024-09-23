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

package views.messaging

import controllers.routes
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import views.DomAssertions
import views.html.messaging.inbox_wrapper
import views.matchers.ViewMatchers

class InboxWrapperSpec extends DomAssertions with ViewMatchers {

  private val partialWrapperPage = instanceOf[inbox_wrapper]
  private val partialContent = "Partial Content"

  private def view: Document = partialWrapperPage(HtmlFormat.raw(partialContent))(fakeRequest, messages)

  "Inbox Wrapper page" should {

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage("inbox.heading")
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(view, routes.InboxChoiceController.onPageLoad.url)
    }

    "display partial contents" in {
      view must containText(partialContent)
    }
  }
}
