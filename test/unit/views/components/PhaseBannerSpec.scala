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

package views.components

import base.{Injector, UnitViewSpec}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.components.phase_banner

class PhaseBannerSpec extends UnitViewSpec with Injector {

  private val bannerPartial = instanceOf[phase_banner]

  private val requestPath = "/customs-declare-exports/any-page"
  private implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", requestPath)

  private def createBanner(): Html = bannerPartial("")(fakeRequest, messages)

  "phaseBanner" should {

    "display feedback link with correct href" in {

      val expectedHrefValue = s"http://localhost:9250/contact/beta-feedback-unauthenticated?service=SFUS&backUrl=http://localhost:6793$requestPath"

      createBanner().getElementsByClass("phase-banner").first().getElementsByTag("a").first() must haveHref(expectedHrefValue)
    }
  }
}
