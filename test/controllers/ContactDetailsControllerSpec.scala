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

import controllers.actions.DataRetrievalAction
import forms.mappings.ContactDetailsMapping._
import models._
import models.requests.SignedInUser
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary._
import play.api.data.Form
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.contact_details

class ContactDetailsControllerSpec extends ControllerSpecBase {

  private val form = Form(contactDetailsMapping)
  val page = mock[contact_details]

  val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-.]+$"""
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get

  def view(form: Form[ContactDetails] = form): String = page(form, mrn)(fakeRequest, messages).toString

  def controller(signedInUser: SignedInUser, eori: String, dataRetrieval: DataRetrievalAction = new FakeDataRetrievalAction(None)) =
    new ContactDetailsController(
      new FakeAuthAction(signedInUser),
      dataRetrieval,
      new FakeMrnRequiredAction,
      new FakeVerifiedEmailAction(),
      mockFileUploadAnswersService,
      mcc,
      page
    )(mcc.executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach

    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)

    super.afterEach()
  }

  "Contact details page" should {

    "load the correct page when user is logged in" in {

      forAll { (user: SignedInUser, eori: String) =>
        val answers = FileUploadAnswers(eori, mrn = Some(mrn))
        val result = controller(user, eori, fakeDataRetrievalAction(answers)).onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }

    "contact details should be displayed if they exist in the cache" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>
        val answers = FileUploadAnswers(eori, mrn = Some(mrn), contactDetails = Some(contactDetails))
        val result = controller(user, eori, fakeDataRetrievalAction(answers)).onPageLoad(fakeRequest)

        contentAsString(result) mustBe view(form.fill(contactDetails))
      }
    }

    "return an see other when valid data is submitted" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>
        whenever(contactDetails.email.matches(emailRegex)) {
          val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(contactDetails): _*)
          val answers = FileUploadAnswers(eori, mrn = Some(mrn))
          val result = controller(user, eori, fakeDataRetrievalAction(answers)).onSubmit(postRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.HowManyFilesUploadController.onPageLoad().url)
        }
      }
    }

    "return a bad request when invalid data is submitted" in {

      forAll(arbitrary[SignedInUser], eoriString, arbitrary[ContactDetails], minStringLength(36)) { (user, eori, contactDetails, invalidName) =>
        val badData = contactDetails.copy(name = invalidName)
        val answers = FileUploadAnswers(eori, mrn = Some(mrn))

        val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(badData): _*)
        val badForm = form.fillAndValidate(badData)

        val result = controller(user, eori, fakeDataRetrievalAction(answers)).onSubmit(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(badForm)
      }
    }

    "save data in cache when valid" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>
        whenever(contactDetails.email.matches(emailRegex)) {
          resetAnswersService()
          val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(contactDetails): _*)
          val answers = FileUploadAnswers(eori, mrn = Some(mrn))

          await(controller(user, eori, fakeDataRetrievalAction(answers)).onSubmit(postRequest))

          theSavedFileUploadAnswers.contactDetails mustBe Some(contactDetails)
        }
      }
    }
  }
}
