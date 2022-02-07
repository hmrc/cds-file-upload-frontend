/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.UpscanConnector
import controllers.actions._
import forms.FileUploadCountProvider
import models._
import models.requests.SignedInUser
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.CustomsDeclarationsService
import utils.FakeRequestCSRFSupport._
import views.DomAssertions
import views.html.how_many_files_upload

import scala.concurrent.Future

class HowManyFilesUploadControllerSpec extends ControllerSpecBase with DomAssertions with GuiceOneAppPerSuite {

  type UserInfo = (SignedInUser, String)

  def zip[A, B](ga: Gen[A], gb: Gen[B]): Gen[(A, B)] =
    ga.flatMap(a => gb.map(b => (a, b)))

  implicit val arbitraryUserInfo: Arbitrary[UserInfo] = Arbitrary(zip(userGen, alphaNumString()))
  val eori: String = eoriString.sample.get
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  private val fileUploadCount = FileUploadCount(7)
  val validAnswers = FileUploadAnswers(eori, mrn = Some(mrn), fileUploadCount = fileUploadCount)

  implicit val arbitraryContactDetailsActions: Arbitrary[ContactDetailsRequiredAction] =
    Arbitrary(arbitrary[FakeContactDetailsRequiredAction].map(_.asInstanceOf[ContactDetailsRequiredAction]))

  implicit val arbitraryFakeContactDetailsActions: Arbitrary[FakeContactDetailsRequiredAction] =
    Arbitrary {
      for {
        details <- arbitrary[ContactDetails]
      } yield {
        new FakeContactDetailsRequiredAction(details)
      }
    }

  private val fakeContactDetailsRequiredAction = arbitraryFakeContactDetailsActions.arbitrary.retryUntil(_ => true).sample.get
  private val mockCustomsDeclarationsService = mock[CustomsDeclarationsService]
  private val mockUpscanConnector = mock[UpscanConnector]

  private val page = mock[how_many_files_upload]

  private def controller(
    contactDetailsRequiredAction: ContactDetailsRequiredAction = new FakeContactDetailsRequiredAction(),
    answers: Option[FileUploadAnswers] = Some(validAnswers)
  ) =
    new HowManyFilesUploadController(
      new FakeAuthAction(),
      new FakeDataRetrievalAction(answers),
      new MrnRequiredActionImpl(mcc),
      contactDetailsRequiredAction,
      new FakeVerifiedEmailAction(),
      new FileUploadCountProvider,
      mockFileUploadAnswersService,
      mockUpscanConnector,
      mockCustomsDeclarationsService,
      mcc,
      page
    )(executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockCustomsDeclarationsService, mockUpscanConnector, page)

    when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(FileUploadResponse(List())))
    when(page(any[Form[FileUploadCount]], any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockCustomsDeclarationsService, mockUpscanConnector, page)

    super.afterEach()
  }

  "How Many Files Upload Page" must {

    "load correct page when user is logged in " in {

      val result = controller().onPageLoad(fakeRequest.withCSRFToken)

      status(result) mustBe OK
      verify(page).apply(any[Form[FileUploadCount]], any())(any(), any())

    }

    "provide view template with correct file count if it exist in the cache" in {
      val updatedAction = new FakeContactDetailsRequiredAction(fakeContactDetailsRequiredAction.contactDetails)

      controller(updatedAction).onPageLoad(fakeRequest.withCSRFToken).futureValue

      val expectedForm = (new FileUploadCountProvider)().fill(fileUploadCount.get)
      verify(page).apply(eqTo(expectedForm), any())(any(), any())
    }

    "redirect to error page when no data is found in the cache on page load" in {

      forAll { contactDetails: ContactDetails =>
        val action = new FakeContactDetailsRequiredAction(contactDetails)
        val result = controller(action, None).onPageLoad(fakeRequest.withCSRFToken)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
      }
    }

    "redirect to error page when no data is found in the cache on submit" in {

      forAll { contactDetails: ContactDetails =>
        val action = new FakeContactDetailsRequiredAction(contactDetails)
        val result = controller(action, None).onSubmit(fakeRequest.withCSRFToken)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
      }
    }

    "return a bad request when empty data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "").withCSRFToken
      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)
      status(result) mustBe BAD_REQUEST
    }

    "return an ok and save to the data cache when valid data is submitted" in {

      val wsResponse = mock[WSResponse]
      when(wsResponse.header("Location")).thenReturn(Some("upscan-success"))
      when(wsResponse.status).thenReturn(303)

      val fileUploadResponse = FileUploadResponse(
        List(
          FileUpload("someFileRef1", Waiting(UploadRequest("http://s3bucket/myfile1", Map("" -> ""))), id = "id1"),
          FileUpload("someFileRef2", Waiting(UploadRequest("http://s3bucket/myfile2", Map("" -> ""))), id = "id2"),
          FileUpload("someFileRef3", Waiting(UploadRequest("http://s3bucket/myfile3", Map("" -> ""))), id = "id3")
        )
      )
      val fileUploadsAfterContactDetails = fileUploadResponse.uploads.tail

      when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(fileUploadResponse))
      when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))

      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2").withCSRFToken

      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      val nextRef = fileUploadsAfterContactDetails.map(_.reference).min
      redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad(nextRef).url)

      val savedAnswers = theSavedFileUploadAnswers
      val Some(fileUploadCount) = FileUploadCount(2)
      savedAnswers.fileUploadCount mustBe Some(fileUploadCount)
      savedAnswers.fileUploadResponse mustBe Some(FileUploadResponse(fileUploadResponse.uploads.tail))
    }

    "redirect to error page when contact details upload fails" in {

      val wsResponse = mock[WSResponse]
      when(wsResponse.header("Location")).thenReturn(Some("upscan-error"))
      when(wsResponse.status).thenReturn(303)

      val fileUploadResponse = FileUploadResponse(
        List(
          FileUpload("someFileRef1", Waiting(UploadRequest("http://s3bucket/myfile1", Map("" -> ""))), id = "id1"),
          FileUpload("someFileRef2", Waiting(UploadRequest("http://s3bucket/myfile2", Map("" -> ""))), id = "id2")
        )
      )
      reset(mockUpscanConnector)
      when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(fileUploadResponse))
      when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))

      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2").withCSRFToken

      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
    }

    "redirect to error page when contact details upscan responds 400" in {

      val wsResponse = mock[WSResponse]
      when(wsResponse.header("Location")).thenReturn(Some("upscan-error"))
      when(wsResponse.status).thenReturn(400)

      val fileUploadResponse = FileUploadResponse(
        List(
          FileUpload("someFileRef1", Waiting(UploadRequest("http://s3bucket/myfile1", Map("" -> ""))), id = "id1"),
          FileUpload("someFileRef2", Waiting(UploadRequest("http://s3bucket/myfile2", Map("" -> ""))), id = "id2")
        )
      )
      reset(mockUpscanConnector)
      when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(fileUploadResponse))
      when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))

      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2").withCSRFToken

      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
    }

    "make a request to customs declarations" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2")
      await(controller(fakeContactDetailsRequiredAction).onSubmit(postRequest.withCSRFToken))

      val Some(fileUploadCount) = FileUploadCount(2)
      verify(mockCustomsDeclarationsService).batchFileUpload(any(), eqTo(mrn), eqTo(fileUploadCount))(any())
    }
  }
}
