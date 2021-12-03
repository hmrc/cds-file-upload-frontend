/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ExternalServicesConfig
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.{undeliverable_email, unverified_email}

class UnverifiedEmailControllerSpec extends ControllerSpecBase {

  val unverifiedPage = mock[unverified_email]
  val undeliverablePage = mock[undeliverable_email]
  val config = instanceOf[ExternalServicesConfig]

  def controller() =
    new UnverifiedEmailController(new FakeAuthAction(), mcc, unverifiedPage, undeliverablePage, config)

  "UnverifiedEmailController" should {
    "display the unverified email detection page" in {
      when(unverifiedPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
      val result = controller().informUserUnverified(fakeRequest)

      status(result) mustBe OK
    }

    "display the undeliverable email detection page" in {
      when(undeliverablePage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
      val result = controller().informUserUndeliverable(fakeRequest)

      status(result) mustBe OK
    }
  }
}
