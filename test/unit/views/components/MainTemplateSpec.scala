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
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.twirl.api.HtmlFormat
import views.Title
import views.html.components.gds.gdsMainTemplate
import views.matchers.ViewMatchers
import base.{OverridableInjector, TestModule}
import config.{Frontend, I18n, Play}

class MainTemplateSpec extends SpecBase with ViewMatchers {

class MainTemplateSpec extends SpecBase with ViewMatchers with BeforeAndAfterEach {

  private val injector = new OverridableInjector(new TestModule(_.copy(play = Play(Frontend(None), I18n(List("en"))))))
  private implicit val mainTemplate = injector.instanceOf[gdsMainTemplate]
  private val testContent = HtmlFormat.empty

  private def createView(withNavigationBanner: Boolean = false)(implicit template: gdsMainTemplate): Document =
    template(title = Title("common.service.name"), withNavigationBanner = withNavigationBanner)(
      testContent
    )(fakeRequest, messages)

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

    "welsh is not in the languages config" should {
      "not display the language toggle" in {
        val view: Document = createView()

        view mustNot containElementWithClass("hmrc-language-select")
      }
    }

    "welsh is in the languages config" should {
      "display the language toggle" in {
        val injectorWithWelsh = new OverridableInjector(new TestModule(_.copy(play = Play(Frontend(None), I18n(List("en", "cy"))))))
        val templateWithWelsh = injectorWithWelsh.instanceOf[gdsMainTemplate]
        val view: Document = createView()(templateWithWelsh)

        view must containElementWithClass("hmrc-language-select")
      }
    }
  }
}
