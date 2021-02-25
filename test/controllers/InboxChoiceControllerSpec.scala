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
import forms.InboxChoiceForm
import forms.InboxChoiceForm.{InboxChoiceKey, Values}
import models.exceptions.InvalidFeatureStateException
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.inbox_choice_page

class InboxChoiceControllerSpec extends ControllerSpecBase with TestRequests {

  private val inboxChoicePage = mock[inbox_choice_page]
  private val secureMessagingFeatureAction = new SecureMessagingFeatureActionMock()

  private val controller =
    new InboxChoiceController(
      stubMessagesControllerComponents(),
      new FakeAuthAction(),
      new FakeVerifiedEmailAction(),
      secureMessagingFeatureAction,
      inboxChoicePage
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(inboxChoicePage)
    secureMessagingFeatureAction.reset()
    when(inboxChoicePage.apply(any[Form[InboxChoiceForm]])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
  }

  "InboxChoiceController on onPageLoad" when {

    "the feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.onPageLoad()(fakeRequest))
        }
      }
    }

    "the feature flag for SecureMessaging is enabled" should {

      "return a 200(OK) status code" in {
        secureMessagingFeatureAction.enableSecureMessagingFeature()

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
      }

      "call inbox_choice_page template" in {
        secureMessagingFeatureAction.enableSecureMessagingFeature()

        controller.onPageLoad()(fakeRequest).futureValue

        verify(inboxChoicePage).apply(any[Form[InboxChoiceForm]])(any[Request[_]], any[Messages])
      }
    }
  }

  "InboxChoiceController on onSubmit" when {

    "the feature flag for SecureMessaging is disabled" should {

      "return a 404(NOT_FOUND) status code" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        val request = postRequest(Json.obj(InboxChoiceKey -> "Test Choice"))

        an[InvalidFeatureStateException] mustBe thrownBy { await(controller.onSubmit()(request)) }
      }
    }

    "the feature flag for SecureMessaging is enabled" when {

      "provided with incorrect form" should {

        val request = postRequest(Json.obj(InboxChoiceKey -> "Incorrect Choice"))

        "return a 400(BAD_REQUEST) status code" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          val result = controller.onSubmit()(request)

          status(result) mustBe BAD_REQUEST
        }

        "call inboxChoicePage passing a form with errors" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          controller.onSubmit()(request).futureValue

          val expectedFormWithErrors = InboxChoiceForm.form.bind(Map(InboxChoiceKey -> "Incorrect Choice"))
          verify(inboxChoicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
        }
      }

      "provided with an empty form" should {

        val request = postRequest(Json.obj(InboxChoiceKey -> ""))

        "return a 400(BAD_REQUEST) status code" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          val result = controller.onSubmit()(request)

          status(result) mustBe BAD_REQUEST
        }

        "call inboxChoicePage passing a form with errors" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()

          controller.onSubmit()(request).futureValue

          val expectedFormWithErrors = InboxChoiceForm.form.bind(Map(InboxChoiceKey -> ""))
          verify(inboxChoicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
        }
      }

      "provided with a correct form" which {

        "specifies the choice to show the Exports Messages" should {

          val request = postRequest(Json.obj(InboxChoiceKey -> Values.ExportsMessages))

          "return SEE_OTHER (303)" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.onSubmit()(request)

            status(result) mustBe SEE_OTHER
          }

          "redirect to /messages" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.onSubmit()(request)

            redirectLocation(result) mustBe Some(controllers.routes.SecureMessagingController.displayInbox.url)
          }
        }

        "specifies the choice to show the Imports Messages" should {

          val request = postRequest(Json.obj(InboxChoiceKey -> Values.ImportsMessages))

          "return SeeOther (303)" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.onSubmit()(request)

            status(result) mustBe SEE_OTHER
          }

          "redirect to /messages" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            val result = controller.onSubmit()(request)

            redirectLocation(result) mustBe Some(controllers.routes.SecureMessagingController.displayInbox.url)
          }
        }
      }
    }
  }

}
