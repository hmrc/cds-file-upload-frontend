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

package views.components.gds

import base.SpecBase
import config.AppConfig
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import views.Title
import views.html.components.gds.gdsMainTemplate
import views.matchers.ViewMatchers

class MainTemplateSpec extends SpecBase with ViewMatchers {

  override implicit lazy val appConfig: AppConfig = instanceOf[AppConfig]
  private val mainTemplate = instanceOf[gdsMainTemplate]
  private val testContent = HtmlFormat.empty

  private def createView(withNavigationBanner: Boolean = false): Document =
    mainTemplate(Title("common.service.name"), withNavigationBanner = withNavigationBanner)(testContent)(fakeRequest, messages)

  "Main Template" should {

    "display the expected title as part of the <head> tag" in {
      val view: Document = createView()
      val serviceName = messages("common.service.name")
      view.getElementsByTag("title").first.text mustBe messages("common.title.format", serviceName, serviceName)
    }

    "display NavigationBanner" when {
      "withNavigationBanner flag set to true" in {
        val view: Document = createView(withNavigationBanner = true)
        view must containElementWithID("navigation-banner")
      }
    }

    "not display NavigationBanner" when {
      "withNavigationBanner flag set to false" in {
        val view: Document = createView()
        view mustNot containElementWithID("navigation-banner")
      }
    }
  }
}
