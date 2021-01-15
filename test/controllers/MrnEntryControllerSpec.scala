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

import controllers.actions.ContactDetailsRequiredAction
import forms.MRNFormProvider
import models.{MRN, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.MrnDisValidator
import testdata.CommonTestData._
import views.html.mrn_entry

class MrnEntryControllerSpec extends ControllerSpecBase {

  private val contactDetailsRequiredGen = new FakeContactDetailsRequiredAction(contactDetails)

  val mrnDisValidator: MrnDisValidator = mock[MrnDisValidator]
  private val page = mock[mrn_entry]

  private val validAnswers = UserAnswers(eori, contactDetails = Some(contactDetails))

  private def mrnEntryController(requireContactDetails: ContactDetailsRequiredAction, answers: UserAnswers = validAnswers) =
    new MrnEntryController(
      new FakeAuthAction(),
      new FakeEORIAction(eori),
      requireContactDetails,
      new FakeDataRetrievalAction(Some(answers)),
      new MRNFormProvider,
      mockAnswersConnector,
      mcc,
      page
    )(executionContext)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(page)
    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(page)

    super.afterEach()
  }

  "MrnEntryController on onPageLoad" should {

    "return Ok (200) response" when {

      "cache is empty" in {

        val controller = mrnEntryController(contactDetailsRequiredGen)
        val result = controller.onPageLoad(fakeRequest)

        status(result) mustBe OK
      }

      "cache contains MRN data" in {

        val controller = mrnEntryController(contactDetailsRequiredGen, validAnswers.copy(mrn = MRN(mrn)))
        val result = controller.onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }
  }

  "MrnEntryController on onSubmit" when {

    val controller = mrnEntryController(contactDetailsRequiredGen)

    "provided with incorrect data" should {

      "return BadRequest (400) response" in {

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "Invalid MRN")

        val result = controller.onSubmit(postRequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "provided with correct data" should {

      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn)

      "call AnswersConnector" in {

        controller.onSubmit(postRequest).futureValue

        val upsertedUserAnswers = theSavedUserAnswers
        upsertedUserAnswers.mrn mustBe defined
        upsertedUserAnswers.mrn.get mustBe MRN(mrn).get
      }

      "return SeeOther (303) response" in {

        val result = controller.onSubmit(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.HowManyFilesUploadController.onPageLoad().url
      }
    }
  }

}
