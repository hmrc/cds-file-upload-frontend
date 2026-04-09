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
import views.html.components.gds.siteHeader

class ServiceNavigationSpec extends UnitSpec with ViewMatchers {

  private val serviceNavigation = instanceOf[siteHeader]

  private val component = serviceNavigation()(request, messages)

  private val componentWithNav = serviceNavigation(showNavigationLinks = true)()

  "Site Header component" should {

    "display the service name" in {
      val serviceNameLink = component.getElementsByClass("govuk-service-navigation__link").get(0)
      serviceNameLink must not be null
      serviceNameLink.text mustBe messages("service.name")
      serviceNameLink.attr("href") mustBe controllers.routes.RootController.displayPage.url
    }

    "display the navigation links correctly" in {
      val navList = componentWithNav.getElementsByClass("govuk-service-navigation__list")
      navList must not be empty

      val navLinks = navList.first().getElementsByTag("a")
      navLinks must have size 2

      val messagesLink = navLinks.get(0)
      messagesLink.text mustBe messages("common.navigation.messages")
      messagesLink.attr("href") mustBe controllers.routes.InboxChoiceController.onPageLoad.url

      val uploadLink = navLinks.get(1)
      uploadLink.text mustBe messages("common.navigation.uploadFiles")
      uploadLink.attr("href") mustBe controllers.routes.MrnEntryController.onPageLoad.url
    }

    "display the language toggle" in {
      val languageNav = component.getElementsByClass("hmrc-service-navigation-language-select")
      languageNav must not be empty

      val listItems = component.getElementsByClass("hmrc-service-navigation-language-select__list-item")
      listItems must have size 2

      val currentLang = listItems.get(0).getElementsByTag("span").get(0)
      currentLang.text mustBe "ENG"
      currentLang.attr("aria-current") mustBe "true"

      val altLangLink = listItems.get(1).getElementsByTag("a").get(0)
      altLangLink.text must include("CYM")
      altLangLink.select("span.govuk-visually-hidden").text() must include("Newid yr iaith i’r Gymraeg")
      altLangLink.attr("href") mustBe "/language/cy"
      altLangLink.attr("hreflang") mustBe "cy"
      altLangLink.attr("lang") mustBe "cy"
      altLangLink.attr("rel") mustBe "alternate"
    }
  }
}
