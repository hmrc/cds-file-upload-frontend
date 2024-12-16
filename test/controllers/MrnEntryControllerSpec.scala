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

import forms.MRNFormProvider
import models.{FileUploadAnswers, MRN}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testdata.CommonTestData._
import views.html.{mrn_access_denied, mrn_entry}

class MrnEntryControllerSpec extends ControllerSpecBase {

  private val mrnEntryPage = mock[mrn_entry]
  private val mrnAccessDeniedPage = mock[mrn_access_denied]

  private val validAnswers = FileUploadAnswers(eori, cacheId, contactDetails = Some(contactDetails))

  private def mrnEntryController(answers: FileUploadAnswers = validAnswers) =
    new MrnEntryController(
      new FakeAuthAction(),
      new FakeDataRetrievalAction(Some(answers)),
      new FakeVerifiedEmailAction(),
      new MRNFormProvider,
      mockFileUploadAnswersService,
      mcc,
      mrnEntryPage,
      mrnAccessDeniedPage
    )(executionContext)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mrnEntryPage, mrnAccessDeniedPage)
    when(mrnEntryPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(mrnAccessDeniedPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  val validRefererUrl = "/cds-file-upload-service/conversation/CDCM/TEST-kWZYP-9cZCw9Dw"

  "MrnEntryController on onPageLoad" should {
    "return Ok (200) response" when {

      "cache is empty" in {
        val controller = mrnEntryController()
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
      }

      "cache contains MRN data" in {
        val controller = mrnEntryController(validAnswers.copy(mrn = MRN(mrn)))
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
      }
    }
  }

  "MrnEntryController on onPageLoad" when {

    "no value is sent in the request" should {

      "not update the mrnPageRefererUrl value in the cache" in {
        val controller = mrnEntryController(validAnswers.copy(mrnPageRefererUrl = Some(validRefererUrl)))

        controller.onPageLoad(fakeRequest).futureValue

      }

      "call MrnEntryPage, providing default back link url" in {
        val controller = mrnEntryController()

        controller.onPageLoad(fakeRequest).futureValue

        verify(mrnEntryPage).apply(any())(any(), any())
      }
    }

    "a valid URL is sent in the request" should {

      "update the mrnPageRefererUrl value in the cache" in {
        val controller = mrnEntryController()

        controller.onPageLoad(fakeRequest).futureValue

      }

      "call MrnEntryPage, providing back link url from cache" in {
        val controller = mrnEntryController()

        controller.onPageLoad(fakeRequest).futureValue

        verify(mrnEntryPage).apply(any())(any(), any())
      }
    }

    "an invalid URL is sent in the request" should {

      "not update the mrnPageRefererUrl value in the cache" in {
        val controller = mrnEntryController()

        controller.onPageLoad(fakeRequest).futureValue

      }

      "call MrnEntryPage, providing default back link url" in {
        val controller = mrnEntryController()

        controller.onPageLoad(fakeRequest).futureValue

        verify(mrnEntryPage).apply(any())(any(), any())
      }
    }
  }

  "MrnEntryController on onSubmit" when {
    val controller = mrnEntryController()

    "provided with incorrect MRN" should {
      "return BadRequest (400) response" in {
        val postRequest = fakePostRequest.withFormUrlEncodedBody("value" -> "Invalid MRN")
        val result = controller.onSubmit(postRequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "provided with correct MRN" should {
      val postRequest = fakePostRequest.withFormUrlEncodedBody("value" -> mrn)

      "call AnswersService" in {
        controller.onSubmit(postRequest).futureValue

        val upsertedUserAnswers = theSavedFileUploadAnswers
        upsertedUserAnswers.mrn mustBe defined
        upsertedUserAnswers.mrn.get mustBe MRN(mrn).get
      }

      "return SeeOther (303) response" in {
        val result = controller.onSubmit(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.ContactDetailsController.onPageLoad.url
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

    "provided with correct MRN" should {

      "call AnswersService" in {
        controller.autoFill(mrn)(fakeRequest).futureValue

        val upsertedUserAnswers = theSavedFileUploadAnswers
        upsertedUserAnswers.mrn mustBe defined
        upsertedUserAnswers.mrn.get mustBe MRN(mrn).get
      }

      "return SeeOther (303) response" in {
        val result = controller.autoFill(mrn)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.ContactDetailsController.onPageLoad.url
      }
    }
  }

}
