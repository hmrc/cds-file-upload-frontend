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

import forms.ChoiceForm
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.FileUploadAnswersService
import scala.concurrent.Future
import views.html.choice_page
import play.api.test.Helpers

class ChoiceControllerSpec extends ControllerSpecBase {

  private val choicePage = mock[choice_page]
  private val answersService = mock[FileUploadAnswersService]

  val ec = Helpers.stubControllerComponents().executionContext

  private val controller =
    new ChoiceController(stubMessagesControllerComponents(), new FakeAuthAction(), new FakeVerifiedEmailAction(), answersService, choicePage, ec)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(choicePage)
    when(choicePage.apply(any[Form[ChoiceForm]])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(choicePage)

    super.afterEach()
  }

  "ChoiceController on onPageLoad" should {

    "return Ok (200) response" in {
      when(answersService.remove(any[String])).thenReturn(Future.successful(()))
      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe OK
    }

    "call choice_page template" in {
      controller.onPageLoad()(fakeRequest).futureValue

      verify(choicePage).apply(any[Form[ChoiceForm]])(any[Request[_]], any[Messages])
    }
  }

  "ChoiceController on onSubmit" when {

    "provided with incorrect form" should {

      val request = fakePostRequest.withFormUrlEncodedBody("choice" -> "Incorrect Choice")

      "return BadRequest (400)" in {
        val result = controller.onSubmit()(request)

        status(result) mustBe BAD_REQUEST
      }

      "call choicePage passing form with errors" in {
        controller.onSubmit()(request).futureValue

        val expectedFormWithErrors = ChoiceForm.form.bind(Map("choice" -> "Incorrect Choice"))
        verify(choicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
      }
    }

    "provided with empty form" should {

      val request = fakePostRequest.withFormUrlEncodedBody("choice" -> "")

      "return BadRequest (400)" in {
        val result = controller.onSubmit()(request)

        status(result) mustBe BAD_REQUEST
      }

      "call choicePage passing form with errors" in {
        controller.onSubmit()(request).futureValue

        val expectedFormWithErrors = ChoiceForm.form.bind(Map("choice" -> ""))
        verify(choicePage).apply(eqTo(expectedFormWithErrors))(any[Request[_]], any[Messages])
      }
    }

    "provided with correct form" which {

      "contains value for Secure Inbox" should {

        val request = fakePostRequest.withFormUrlEncodedBody("choice" -> ChoiceForm.AllowedChoiceValues.SecureMessageInbox)

        "return SEE_OTHER (303)" in {
          val result = controller.onSubmit()(request)

          status(result) mustBe SEE_OTHER
        }

        "redirect to /what-messages-to-view" in {
          val result = controller.onSubmit()(request)

          redirectLocation(result) mustBe Some(controllers.routes.InboxChoiceController.onPageLoad.url)
        }
      }

      "contains value for submitting documents" should {

        val request = fakePostRequest.withFormUrlEncodedBody("choice" -> ChoiceForm.AllowedChoiceValues.DocumentUpload)

        "return SeeOther (303)" in {
          val result = controller.onSubmit()(request)

          status(result) mustBe SEE_OTHER
        }

        "redirect to /mrn-entry" in {
          val result = controller.onSubmit()(request)

          redirectLocation(result) mustBe Some(controllers.routes.MrnEntryController.onPageLoad().url)
        }
      }
    }
  }
}
