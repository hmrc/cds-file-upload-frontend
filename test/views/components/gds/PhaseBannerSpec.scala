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

import config.{AppConfig, Frontend, I18n, Play}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.govukfrontend.views.html.components.GovukPhaseBanner
import views.base.UnitViewSpec
import views.html.components.gds.phaseBanner

class PhaseBannerSpec extends UnitViewSpec {

  private val appConfig: AppConfig = instanceOf[AppConfig]

  private val govukPhaseBanner = instanceOf[GovukPhaseBanner]

  private val requestUri = "/customs-declare-exports/any-page"

  private def createComponent(appConfig: AppConfig): Appendable = {
    val phaseBanner = new phaseBanner(govukPhaseBanner, appConfig)
    phaseBanner("")(FakeRequest("GET", requestUri), messages)
  }

  "phaseBanner" should {

    "display feedback link with correct href" when {

      val giveFeedbackLink = appConfig.microservice.services.contactFrontend.giveFeedbackLink

      "appConfig.play.frontend.host is defined" in {
        val expectedHost = "http://localhost:6793"

        val element = createComponent(appConfig).getElementsByClass("govuk-phase-banner__text").first
        val href = s"$giveFeedbackLink&backUrl=$expectedHost$requestUri"
        element.getElementsByTag("a").first must haveHref(href)
      }

      "appConfig.play.frontend.host is not defined" in {
        val configNoHost = appConfig.copy(play = Play(Frontend(None), I18n(List("en"))))
        val element = createComponent(configNoHost).getElementsByClass("govuk-phase-banner__text").first
        element.getElementsByTag("a").first must haveHref(s"$giveFeedbackLink")
      }
    }
  }
}
