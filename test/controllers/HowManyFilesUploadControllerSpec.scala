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

import connectors.UpscanConnector
import controllers.actions._
import controllers.routes.ErrorPageController
import models._
import models.requests.SignedInUser
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import services.CustomsDeclarationsService
import testdata.CommonTestData.cacheId
import utils.FakeRequestCSRFSupport._

import scala.concurrent.Future

class HowManyFilesUploadControllerSpec extends ControllerSpecBase {

  type UserInfo = (SignedInUser, String)

  def zip[A, B](ga: Gen[A], gb: Gen[B]): Gen[(A, B)] =
    ga.flatMap(a => gb.map(b => (a, b)))

  implicit val arbitraryUserInfo: Arbitrary[UserInfo] = Arbitrary(zip(userGen, alphaNumString()))
  val eori: String = eoriString.sample.get
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  val validAnswers = FileUploadAnswers(eori, cacheId, mrn = Some(mrn))

  implicit val arbitraryContactDetailsActions: Arbitrary[ContactDetailsRequiredAction] =
    Arbitrary(arbitrary[FakeContactDetailsRequiredAction].map(_.asInstanceOf[ContactDetailsRequiredAction]))

  implicit val arbitraryFakeContactDetailsActions: Arbitrary[FakeContactDetailsRequiredAction] =
    Arbitrary {
      for {
        details <- arbitrary[ContactDetails]
      } yield new FakeContactDetailsRequiredAction(details)
    }

  private val fakeContactDetailsRequiredAction = arbitraryFakeContactDetailsActions.arbitrary.retryUntil(_ => true).sample.get
  private val mockCustomsDeclarationsService = mock[CustomsDeclarationsService]
  private val mockUpscanConnector = mock[UpscanConnector]

  private val contactDetailsBatch = FileUploadResponse(
    List(FileUpload("contactDetailsRef", Waiting(UploadRequest("http://s3bucket/contact", Map("" -> ""))), id = "id1"))
  )

  private def controller(contactDetailsRequiredAction: ContactDetailsRequiredAction, answers: Option[FileUploadAnswers] = Some(validAnswers)) =
    new HowManyFilesUploadController(
      new FakeAuthAction(),
      new FakeDataRetrievalAction(answers),
      new MrnRequiredActionImpl(mcc),
      contactDetailsRequiredAction,
      new FakeVerifiedEmailAction(),
      mockFileUploadAnswersService,
      mockUpscanConnector,
      mockCustomsDeclarationsService,
      mcc
    )(executionContext)

  private def stubSuccessfulContactDetailsUpload(): Unit = {
    val wsResponse = mock[WSResponse]
    when(wsResponse.header("Location")).thenReturn(Some("upscan-success"))
    when(wsResponse.status).thenReturn(303)
    when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockCustomsDeclarationsService, mockUpscanConnector)

    when(mockCustomsDeclarationsService.initiateSingleFileBatch(any(), any())(any()))
      .thenReturn(Future.successful(contactDetailsBatch))
  }

  "How Many Files Upload Page" must {

    "initiate a single file batch, upload contact details and redirect to the upload page" in {
      stubSuccessfulContactDetailsUpload()

      val result = controller(fakeContactDetailsRequiredAction).onPageLoad(fakeRequest.withCSRFToken)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad.url)

      theSavedFileUploadAnswers.fileUploadResponse mustBe Some(FileUploadResponse(Nil))
    }

    "request the batch for the declaration's mrn" in {
      stubSuccessfulContactDetailsUpload()

      await(controller(fakeContactDetailsRequiredAction).onPageLoad(fakeRequest.withCSRFToken))

      verify(mockCustomsDeclarationsService).initiateSingleFileBatch(any(), eqTo(mrn))(any())
    }

    "redirect to error page when no data is found in the cache" in {
      forAll { (contactDetails: ContactDetails) =>
        val action = new FakeContactDetailsRequiredAction(contactDetails)
        val result = controller(action, None).onPageLoad(fakeRequest.withCSRFToken)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(ErrorPageController.error.url)
      }
    }

    "redirect to error page when contact details upload fails" in {
      val wsResponse = mock[WSResponse]
      when(wsResponse.header("Location")).thenReturn(Some("upscan-error"))
      when(wsResponse.status).thenReturn(303)
      when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))

      val result = controller(fakeContactDetailsRequiredAction).onPageLoad(fakeRequest.withCSRFToken)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ErrorPageController.error.url)
    }

    "redirect to error page when contact details upscan responds 400" in {
      val wsResponse = mock[WSResponse]
      when(wsResponse.header("Location")).thenReturn(Some("upscan-error"))
      when(wsResponse.status).thenReturn(400)
      when(mockUpscanConnector.upload(any(), any())).thenReturn(Future.successful(wsResponse))

      val result = controller(fakeContactDetailsRequiredAction).onPageLoad(fakeRequest.withCSRFToken)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ErrorPageController.error.url)
    }

    "redirect to error page when the batch does not contain exactly one waiting file" in {
      when(mockCustomsDeclarationsService.initiateSingleFileBatch(any(), any())(any()))
        .thenReturn(Future.successful(FileUploadResponse(Nil)))

      val result = controller(fakeContactDetailsRequiredAction).onPageLoad(fakeRequest.withCSRFToken)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ErrorPageController.error.url)
    }
  }
}
