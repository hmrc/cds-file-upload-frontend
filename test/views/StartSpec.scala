/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.routes
import views.behaviours.ViewBehaviours
import views.html.start

class StartSpec extends DomAssertions with ViewBehaviours {

  val view = () => start()(fakeRequest, messages, appConfig)

  val messageKeyPrefix = "startPage"

  "Start Page" must {
    behave like normalPage(view, messageKeyPrefix, "paragraph2", "p.youWillNeed", "listItem1", "listItem2", "listItem3")
    "have a start button with correct link" in {
      val doc = asDocument(view())
      val expectedLink = routes.ContactDetailsController.onPageLoad().url

      assertContainsLink(doc, messages("common.button.startNow"), expectedLink)
    }

    "have paragraph3 with bold text" in {
      val paragraph3 = messages("startPage.paragraph3", messages("startPage.paragraph3.bold"))
      val doc = asDocument(view())

      assertContainsText(doc, paragraph3)
    }

    "have paragraph4 with bold text" in {
      val paragraph4 = messages("startPage.paragraph4", messages("startPage.paragraph4.bold"))
      val doc = asDocument(view())

      assertContainsText(doc, paragraph4)
    }
  }
}
