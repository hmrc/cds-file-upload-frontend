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

package views

import config.ExternalServicesConfig
import controllers.routes
import views.behaviours.ViewBehaviours
import views.html.signed_out

class SignedOutSpec extends DomAssertions with ViewBehaviours {

  val page = instanceOf[signed_out]

  val view = asDocument(page()(fakeRequest, messages))

  val messageKeyPrefix = "signedOut"

  "Signed-Out Page" must {

    behave like normalPage(() => view, messageKeyPrefix)

    "display the expected page header" in {
      view.getElementsByTag("h1").text() mustBe messages("signedOut.heading")
    }

    "display information and link to the Start page" in {
      assertContainsText(view, messages("signedOut.information", messages("signedOut.startPageLink")))
      assertContainsLink(view, messages("signedOut.startPageLink"), routes.RootController.displayPage.url)
    }

    "display feedback-related information" in {
      assertContainsText(view, messages("feedback.header"))
      assertContainsText(view, messages("feedback.line.1"))

      val config = instanceOf[ExternalServicesConfig]
      assertContainsLink(view, messages("feedback.link"), config.feedbackFrontend)
      assertContainsText(view, messages("feedback.line.2", messages("feedback.link")))
    }

    "display the 'Back to GOV.UK' link" in {
      assertContainsLink(view, messages("signedOut.backToGovUk"), "https://www.gov.uk/")
    }
  }
}
