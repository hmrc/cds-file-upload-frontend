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

import forms.MRNFormProvider
import models.{MRN, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.MrnDisValidator
import testdata.CommonTestData._
import views.html.{mrn_access_denied, mrn_entry}

import scala.concurrent.Future

class MrnEntryControllerSpec extends ControllerSpecBase {

  private val mrnDisValidator = mock[MrnDisValidator]
  private val mrnEntryPage = mock[mrn_entry]
  private val mrnAccessDeniedPage = mock[mrn_access_denied]

  private val validAnswers = UserAnswers(eori, contactDetails = Some(contactDetails))

  private def mrnEntryController(answers: UserAnswers = validAnswers) =
    new MrnEntryController(
      new FakeAuthAction(),
      new FakeDataRetrievalAction(Some(answers)),
      new FakeVerifiedEmailAction(),
      new MRNFormProvider,
      mockAnswersConnector,
      mcc,
      mrnEntryPage,
      mrnDisValidator,
      mrnAccessDeniedPage
    )(executionContext)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mrnEntryPage, mrnAccessDeniedPage, mrnDisValidator)
    when(mrnEntryPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(mrnAccessDeniedPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(mrnEntryPage, mrnAccessDeniedPage, mrnDisValidator)

    super.afterEach()
  }

  "MrnEntryController on onPageLoad" should {

    "return Ok (200) response" when {

      "cache is empty" in {

        val controller = mrnEntryController()
        val result = controller.onPageLoad(fakeRequest)

        status(result) mustBe OK
      }

      "cache contains MRN data" in {

        val controller = mrnEntryController(validAnswers.copy(mrn = MRN(mrn)))
        val result = controller.onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }
  }

  "MrnEntryController on onSubmit" when {

    val controller = mrnEntryController()

    "provided with incorrect MRN" should {

      "return BadRequest (400) response" in {

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "Invalid MRN")

        val result = controller.onSubmit(postRequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "provided with correct MRN" which {

      "fails MRN DIS validation" should {

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn)

        "return BadRequest (400) response" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(false))

          val result = controller.onSubmit(postRequest)

          status(result) mustBe BAD_REQUEST
          verify(mrnAccessDeniedPage).apply(any())(any(), any())
        }
      }

      "passes MRN DIS validation" should {

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn)

        "call AnswersConnector" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(true))

          controller.onSubmit(postRequest).futureValue

          val upsertedUserAnswers = theSavedUserAnswers
          upsertedUserAnswers.mrn mustBe defined
          upsertedUserAnswers.mrn.get mustBe MRN(mrn).get
        }

        "return SeeOther (303) response" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(true))

          val result = controller.onSubmit(postRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe routes.ContactDetailsController.onPageLoad().url
        }
      }
    }
  }

  "MrnEntryController on autoFill" when {
    val controller = mrnEntryController()

    "provided with incorrect MRN" should {

      "return BadRequest (400) response" in {

        val result = controller.autoFill("badMrn")(fakeRequest)

        status(result) mustBe BAD_REQUEST
        verify(mrnAccessDeniedPage).apply(any())(any(), any())
      }
    }

    "provided with correct MRN" which {

      "fails MRN DIS validation" should {

        "return BadRequest (400) response" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(false))

          val result = controller.autoFill(mrn)(fakeRequest)

          status(result) mustBe BAD_REQUEST
          verify(mrnAccessDeniedPage).apply(any())(any(), any())
        }
      }

      "passes MRN DIS validation" should {
        "call AnswersConnector" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(true))

          controller.autoFill(mrn)(fakeRequest).futureValue

          val upsertedUserAnswers = theSavedUserAnswers
          upsertedUserAnswers.mrn mustBe defined
          upsertedUserAnswers.mrn.get mustBe MRN(mrn).get
        }

        "return SeeOther (303) response" in {

          when(mrnDisValidator.validate(any(), any())(any())).thenReturn(Future.successful(true))

          val result = controller.autoFill(mrn)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe routes.ContactDetailsController.onPageLoad().url
        }
      }
    }
  }
}
