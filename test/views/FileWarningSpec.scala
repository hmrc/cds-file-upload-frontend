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
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.file_warning

class FileWarningSpec extends DomAssertions with ViewBehaviours {

  val view: () => Html = () => file_warning()(fakeRequest, messages, appConfig)

  val messageKeyPrefix = "fileWarning"

  val messageKeys = List("paragraph1", "paragraph2", "panel")

  "File Warning Page" must {
    behave like normalPage(view, messageKeyPrefix, messageKeys: _*)

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

    "have I understand button with correct link" in {
      val fileWarningView = asDocument(view())
      val expectedLink = routes.HowManyFilesUploadController.onPageLoad().url

      assertContainsLink(fileWarningView, messages("fileWarning.button"), expectedLink)
    }
  }
}
