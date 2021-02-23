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

import config.SecureMessagingConfig
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.start

class StartControllerSpec extends ControllerSpecBase {

  private val page = mock[start]
  private val secureMessagingConfig = mock[SecureMessagingConfig]

  private def controller() =
    new StartController(new FakeAuthAction(), mcc, page, secureMessagingConfig)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(page, secureMessagingConfig)
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page, secureMessagingConfig)

    super.afterEach()
  }

  "StartController on onStart" when {

    "SecureMessaging feature is enabled" should {

      "redirect to Choice page" in {
        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

        val result = controller().onStart(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ChoiceController.displayPage().url)
      }
    }

    "SecureMessaging feature is disabled" should {

      "redirect to MRN Entry page" in {
        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(false)

        val result = controller().onStart(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.MrnEntryController.onPageLoad().url)
      }
    }
  }
}
