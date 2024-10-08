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

package controllers

import models.UnauthorisedReason.{UrlDirect, UserIsAgent}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.{unauthorised, unauthorisedAgent}

class UnauthorisedControllerSpec extends ControllerSpecBase {

  val page = mock[unauthorised]
  val agentPage = mock[unauthorisedAgent]

  def controller = new UnauthorisedController(mcc, page, agentPage)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)

    super.afterEach()
  }

  "controller.onPageLoad" should {

    "return 200" in {
      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
      verify(page).apply()(any(), any())
    }

  }

  "onAgentKickOut method is invoked and" when {

    "User has agent affinity group" in {
      when(agentPage(any())(any(), any())).thenReturn(HtmlFormat.empty)
      val result = controller.onAgentKickOut(UserIsAgent)(fakeRequest)
      status(result) must be(OK)
    }

    "someone travels to the URL directly" in {
      when(agentPage(any())(any(), any())).thenReturn(HtmlFormat.empty)
      val result = controller.onAgentKickOut(UrlDirect)(fakeRequest)
      status(result) must be(OK)
    }
  }
}
