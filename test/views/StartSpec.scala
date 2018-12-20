/*
 * Copyright 2018 HM Revenue & Customs
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

class StartSpec extends ViewSpecBase with ViewBehaviours {

  val view = () => start()(fakeRequest, messages, appConfig)

  val messageKeyPrefix = "startPage"

  "Start Page" must {
    behave like normalPage(view, messageKeyPrefix)

    "have a start button with correct link" in {
      val doc = asDocument(view())
      val expectedLink = routes.MrnEntryController.onPageLoad().url

      assertContainsLink(doc, messages("common.button.startNow"), expectedLink)
    }
  }

}

