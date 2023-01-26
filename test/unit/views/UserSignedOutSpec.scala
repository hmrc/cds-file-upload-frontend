/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ExternalServicesConfig
import controllers.routes
import views.behaviours.ViewBehaviours
import views.html.user_signed_out

class UserSignedOutSpec extends DomAssertions with ViewBehaviours {

  val page = instanceOf[user_signed_out]

  def view(messageKey: String = "signed.out.heading") =
    asDocument(page(messageKey)(fakeRequest, messages))

  val messageKeyPrefix = "signed.out"

  "Signed-Out Page" must {

    behave like normalPage(() => view(), messageKeyPrefix)

    "display the expected page header" in {
      view().getElementsByTag("h1").text() mustBe messages("signed.out.heading")

      val viewOnTimeout = view("session.timeout.heading")
      viewOnTimeout.getElementsByTag("h1").text() mustBe messages("session.timeout.heading")
    }

    "display information and link to the Start page" in {
      assertContainsText(view(), messages("signed.out.information", messages("signed.out.startPageLink")))
      assertContainsLink(view(), messages("signed.out.startPageLink"), routes.RootController.displayPage.url)
    }

    "display feedback-related information" in {
      assertContainsText(view(), messages("feedback.header"))
      assertContainsText(view(), messages("feedback.line.1"))

      val config = instanceOf[ExternalServicesConfig]
      assertContainsLink(view(), messages("feedback.link"), config.feedbackFrontend)
      assertContainsText(view(), messages("feedback.line.2", messages("feedback.link")))
    }

    "display the 'Back to GOV.UK' link" in {
      assertContainsLink(view(), messages("signed.out.backToGovUk"), "https://www.gov.uk")
    }
  }
}
