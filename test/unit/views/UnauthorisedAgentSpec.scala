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

package unit.views

import base.SpecBase
import views.html.unauthorisedAgent
import views.matchers.ViewMatchers

class UnauthorisedAgentSpec extends SpecBase with ViewMatchers {

  lazy val redirectUrl = "/some/url"

  val page = instanceOf[unauthorisedAgent]

  val view = () => page()(fakeRequest, messages)

  val messageKeyPrefix = "unauthorisedAgent"

  "Unverified Email Page" must {

    "display page header" in {
      view().getElementsByTag("h1").first() must containMessage(s"$messageKeyPrefix.heading")
    }

    "have paragraphs with text" in {
      view().getElementsByClass("govuk-body").first() must containMessage(s"${messageKeyPrefix}.paragraph.1")
      view().getElementsByClass("govuk-body").get(1) must containMessage(s"${messageKeyPrefix}.paragraph.2")
    }

  }
}
