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

import org.mockito.Mockito.when
import play.api.test.Helpers._

class RootControllerSpec extends ControllerSpecBase {

  private val controller = new RootController(stubMessagesControllerComponents(), secureMessagingConfig)

  "Root Controller" when {

    "SecureMessaging feature is enabled" should {

      "redirect to Choice page" in {
        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ChoiceController.onPageLoad().url)
      }
    }

    "SecureMessaging feature is enabled" should {

      "redirect to MrnEntry page" in {
        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(false)

        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.MrnEntryController.onPageLoad().url)
      }
    }
  }
}
