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

import controllers.routes.SignOutController
import models.SignOutReason
import play.twirl.api.Html
import testdata.CommonTestData
import views.base.UnitViewSpec
import views.html.mrn_access_denied

class MrnAccessDeniedSpec extends UnitViewSpec {

  private val mrnAccessDeniedPage = instanceOf[mrn_access_denied]
  private def view(mrn: String): Html = mrnAccessDeniedPage(mrn)(request, messages)

  "MrnAccessDenied page" should {

    val testView = view(CommonTestData.mrn)

    "display page header with provided MRN" in {
      testView.getElementsByTag("h1").first() must containMessage("mrnAccessDenied.heading", CommonTestData.mrn)
    }

    "display 2 paragraphs" in {
      val paragraphs = testView.getElementsByClass("govuk-body")

      paragraphs.size() mustBe 2
      paragraphs.get(0) must containMessage("mrnAccessDenied.paragraph.1")
      paragraphs.get(1) must containMessage("mrnAccessDenied.paragraph.2")
    }

    "display additional 'Sign out' button" in {
      val signOutButton = testView.getElementsByClass("govuk-button").first()

      signOutButton must containMessage("signOut.link")
      signOutButton must haveHref(SignOutController.signOut(SignOutReason.UserAction))
    }

    "display link to /mrn-entry page" in {
      val link = testView.getElementsByClass("govuk-link").get(1)

      link must containMessage("mrnAccessDenied.link.enterDifferentMrn")
      link must haveHref(controllers.routes.MrnEntryController.onPageLoad)
    }
  }
}
