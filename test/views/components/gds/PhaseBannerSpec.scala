/*
 * Copyright 2020 HM Revenue & Customs
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

import base.{OverridableInjector, UnitViewSpec}
import base.AppConfigMockHelper._
import config._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.components.gds.phaseBanner

class PhaseBannerSpec extends UnitViewSpec with BeforeAndAfterEach {

  private val appConfigFrontend = mock[Frontend]
  private val appConfigContactFrontend = mock[ContactFrontend]

  private val requestPath = "/customs-declare-exports/any-page"
  private implicit val fakeRequest = FakeRequest("GET", requestPath)

  private val selfBaseUrlTest = "selfBaseUrlTest"
  private val giveFeedbackLinkTest = "giveFeedbackLinkTest"

  private val appConfig = generateMockConfig(
    microservice = Microservice(generateMockServices(contactFrontend = appConfigContactFrontend)),
    platform = Platform(appConfigFrontend)
  )
  private val injector = new OverridableInjector(bind[AppConfig].toInstance(appConfig))

  private val bannerPartial = injector.instanceOf[phaseBanner]

  private def createBanner(): Html = bannerPartial("")(fakeRequest, messages)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfigFrontend)
    when(appConfigFrontend.host).thenReturn(Some(selfBaseUrlTest))

    reset(appConfigContactFrontend)
    when(appConfigContactFrontend.giveFeedbackLink).thenReturn(giveFeedbackLinkTest)
  }

  override def afterEach(): Unit = {
    reset(appConfigFrontend)
    reset(appConfigContactFrontend)
    super.afterEach()
  }

  "phaseBanner" should {

    "display feedback link with correct href" when {

      "selfBaseUrl is defined" in {
        createBanner().getElementsByClass("govuk-phase-banner__text").first().getElementsByTag("a").first() must haveHref(
          s"$giveFeedbackLinkTest&backUrl=$selfBaseUrlTest$requestPath"
        )
      }

      "selfBaseUrl is not defined" in {
        when(appConfig.platform.frontend.host).thenReturn(None)
        createBanner().getElementsByClass("govuk-phase-banner__text").first().getElementsByTag("a").first() must haveHref(s"$giveFeedbackLinkTest")
      }
    }
  }
}
