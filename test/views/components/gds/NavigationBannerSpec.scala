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

package views.components.gds

import base.UnitSpec
import views.base.ViewMatchers
import views.html.components.gds.navigationBanner

class NavigationBannerSpec extends UnitSpec with ViewMatchers {

  private val navigationBanner = instanceOf[navigationBanner]

  private val component = navigationBanner()(messages)

  "Navigation Banner component" should {

    "wrap all links by a <nav> tag" in {
      val navigationBanner = component.getElementById("navigation-banner")
      navigationBanner.tagName mustBe "nav"
      navigationBanner.childrenSize mustBe 2
    }

    "contain link to /message-choice page" in {
      val messagesLink = component.getElementsByClass("govuk-link").get(0)

      messagesLink must containMessage("common.navigation.messages")
      messagesLink must haveHref(controllers.routes.InboxChoiceController.onPageLoad)
    }

    "contain link to /mrn-entry page" in {
      val messagesLink = component.getElementsByClass("govuk-link").get(1)

      messagesLink must containMessage("common.navigation.uploadFiles")
      messagesLink must haveHref(controllers.routes.MrnEntryController.onPageLoad)
    }
  }
}
