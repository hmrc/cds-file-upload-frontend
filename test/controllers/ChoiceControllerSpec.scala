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

import base.TestRequests
import forms.ChoiceForm
import models.exceptions.InvalidFeatureStateException
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.choice_page

class ChoiceControllerSpec extends ControllerSpecBase with TestRequests {

  private val choicePage = mock[choice_page]
  private val secureMessagingFeatureAction = new SecureMessagingFeatureActionMock()

  private val controller =
    new ChoiceController(
      stubMessagesControllerComponents(),
      new FakeAuthAction(),
      new FakeVerifiedEmailAction(),
      secureMessagingFeatureAction,
      choicePage
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(choicePage)
    secureMessagingFeatureAction.reset()
    when(choicePage.apply(any[Form[ChoiceForm]])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(choicePage)
    secureMessagingFeatureAction.reset()

    super.afterEach()
  }

  "ChoiceController on displayPage" when {

    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.displayPage()(fakeRequest))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "return Ok (200) response" in {
        secureMessagingFeatureAction.enableSecureMessagingFeature()

        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe OK
      }

      "call choice_page template" in {
        secureMessagingFeatureAction.enableSecureMessagingFeature()

        controller.displayPage()(fakeRequest).futureValue

        verify(choicePage).apply(any[Form[ChoiceForm]])(any[Request[_]], any[Messages])
      }
    }
  }

  "ChoiceController on submitChoice" when {

    "feature flag for SecureMessaging is disabled" should {

      "return NotFound (404) response" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()
        val request = postRequest(Json.obj("choice" -> "Test Choice"))

        an[InvalidFeatureStateException] mustBe thrownBy { await(controller.submitChoice()(request)) }
      }
    }

    "feature flag for SecureMessaging is enabled" when {

      "provided with incorrect form" should {

        val request = postRequest(Json.obj("choice" -> "Incorrect Choice"))

        "return BadRequest (400)" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          val result = controller.submitChoice()(request)

          status(result) mustBe BAD_REQUEST
        }

        "call choicePage passing form with errors" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          controller.submitChoice()(request).futureValue

          val expectedFormWithErrors = ChoiceForm.form.bind(Map("choice" -> "Incorrect Choice"))
          verify(choicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
        }
      }

      "provided with empty form" should {

        val request = postRequest(Json.obj("choice" -> ""))

        "return BadRequest (400)" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          val result = controller.submitChoice()(request)

          status(result) mustBe BAD_REQUEST
        }

        "call choicePage passing form with errors" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          controller.submitChoice()(request).futureValue

          val expectedFormWithErrors = ChoiceForm.form.bind(Map("choice" -> ""))
          verify(choicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
        }
      }

      "provided with correct form" which {

        "contains value for Secure Inbox" should {

          val request = postRequest(Json.obj("choice" -> ChoiceForm.AllowedChoiceValues.SecureMessageInbox))

          "return Ok (200)" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.submitChoice()(request)

            status(result) mustBe SEE_OTHER
          }

          "call placeholder page" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.submitChoice()(request)

            redirectLocation(result) mustBe Some(controllers.routes.SecureMessagingController.displayInbox().url)
          }
        }

        "contains value for submitting documents" should {

          val request = postRequest(Json.obj("choice" -> ChoiceForm.AllowedChoiceValues.DocumentUpload))

          "return SeeOther (303)" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.submitChoice()(request)

            status(result) mustBe SEE_OTHER
          }

          "redirect to /mrn-entry" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.submitChoice()(request)

            redirectLocation(result) mustBe Some(controllers.routes.MrnEntryController.onPageLoad().url)
          }
        }
      }
    }
  }
}
