/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.routes.SecureMessagingController
import forms.InboxChoiceForm
import forms.InboxChoiceForm.{InboxChoiceKey, Values}
import models.requests.{AuthenticatedRequest, VerifiedEmailRequest}
import models.{ExportMessages, SecureMessageAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.SecureMessageAnswersService
import testdata.CommonTestData.{signedInUser, verifiedEmail}
import views.html.messaging.inbox_choice

import scala.concurrent.Future

class InboxChoiceControllerSpec extends ControllerSpecBase {

  private val inboxChoice = mock[inbox_choice]
  private val secureMessageAnswersService = mock[SecureMessageAnswersService]

  private val controller =
    new InboxChoiceController(
      stubMessagesControllerComponents(),
      new FakeAuthAction(),
      new FakeVerifiedEmailAction(),
      secureMessageAnswersService,
      inboxChoice,
      executionContext
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(inboxChoice, secureMessageAnswersService)
    when(inboxChoice.apply(any[Form[InboxChoiceForm]])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
    when(secureMessageAnswersService.findOneAndReplace(any())).thenReturn(Future.successful(SecureMessageAnswers("", ExportMessages)))
  }

  "InboxChoiceController.onPageLoad" should {

    "return a 200(OK) status code" in {
      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "call inbox_choice_page template" in {
      controller.onPageLoad()(fakeRequest).futureValue
      verify(inboxChoice).apply(any[Form[InboxChoiceForm]])(any[Request[_]], any[Messages])
    }
  }

  "InboxChoiceController.onSubmit" when {

    "provided with incorrect form" should {
      val request = fakePostRequest.withFormUrlEncodedBody(InboxChoiceKey -> "Incorrect Choice")

      "return a 400(BAD_REQUEST) status code" in {
        val result = controller.onSubmit()(request)
        status(result) mustBe BAD_REQUEST
      }

      "call the inbox_choice page passing a form with errors" in {
        controller.onSubmit()(request).futureValue

        val expectedFormWithErrors = InboxChoiceForm.form.bind(Map(InboxChoiceKey -> "Incorrect Choice"))
        verify(inboxChoice).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
      }
    }

    "provided with an empty form" should {
      val request = fakePostRequest.withFormUrlEncodedBody(InboxChoiceKey -> "")

      "return a 400(BAD_REQUEST) status code" in {
        val result = controller.onSubmit()(request)
        status(result) mustBe BAD_REQUEST
      }

      "call the inbox_choice page passing a form with errors" in {
        controller.onSubmit()(request).futureValue

        val expectedFormWithErrors = InboxChoiceForm.form.bind(Map(InboxChoiceKey -> ""))
        verify(inboxChoice).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
      }
    }

    "provided with a correct form" which {

      "specifies the choice to show the Exports Messages" should {
        val request = fakePostRequest.withFormUrlEncodedBody(InboxChoiceKey -> Values.ExportsMessages)

        "call SecureMessageAnswerService" in {
          controller.onSubmit()(request).futureValue
          verify(secureMessageAnswersService).findOneAndReplace(SecureMessageAnswers(any(), ExportMessages))
        }

        "return SEE_OTHER (303) and redirect to /messages" in {
          val result = controller.onSubmit()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(SecureMessagingController.displayInbox.url)
        }
      }

      "specifies the choice to show the Imports Messages" should {
        val request = fakePostRequest.withFormUrlEncodedBody(InboxChoiceKey -> Values.ImportsMessages)

        "return SEE_OTHER (303) and redirect to /messages" in {
          val result = controller.onSubmit()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(SecureMessagingController.displayInbox.url)
        }
      }
    }
  }

  "InboxChoiceController.onExportsMessageChoice" should {

    "call SecureMessageAnswerService" in {
      controller.onExportsMessageChoice()(fakeGetRequest).futureValue
      verify(secureMessageAnswersService).findOneAndReplace(SecureMessageAnswers(any(), ExportMessages))
    }

    "return SEE_OTHER (303) and redirect to /messages" in {
      val result = controller.onExportsMessageChoice()(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(SecureMessagingController.displayInbox.url)
    }
  }

  "InboxChoiceController.checkMessageFilterTag" should {
    "return a 400(BAD_REQUEST) status code" when {
      "the given parameter is not a 'MessageFilterTag' value" in {
        val request = VerifiedEmailRequest(AuthenticatedRequest(fakeRequest, signedInUser), verifiedEmail)
        val result = controller.checkMessageFilterTag("some choice")(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
