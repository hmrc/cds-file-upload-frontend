/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.actions._
import forms.FileUploadCountProvider
import models._
import models.requests.SignedInUser
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import pages.{HowManyFilesUploadPage, MrnEntryPage}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsNumber, JsString}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import services.CustomsDeclarationsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import views.DomAssertions

import scala.concurrent.Future
import scala.util.{Failure, Success}

class HowManyFilesUploadControllerSpec extends ControllerSpecBase with DomAssertions {

  type UserInfo = (SignedInUser, String)

  def zip[A, B](ga: Gen[A], gb: Gen[B]): Gen[(A, B)] =
    ga.flatMap(a => gb.map(b => (a, b)))

  implicit val arbitraryUserInfo: Arbitrary[UserInfo] = Arbitrary(zip(userGen, arbitrary[String]))

  implicit val arbitraryMrnCacheMap: Arbitrary[CacheMap] =
    Arbitrary {
      zip(arbitraryCacheMap.arbitrary, arbitraryMrn.arbitrary).map {
        case (cacheMap, mrn) =>
          cacheMap.copy(data = cacheMap.data + (MrnEntryPage.toString -> JsString(mrn.value)))
      }
    }

  implicit val arbitraryContactDetailsActions: Arbitrary[ContactDetailsRequiredAction] =
    Arbitrary(arbitrary[FakeContactDetailsRequiredAction].map(_.asInstanceOf[ContactDetailsRequiredAction]))

  implicit val arbitraryFakeContactDetailsActions: Arbitrary[FakeContactDetailsRequiredAction] =
    Arbitrary {
      for {
        details <- arbitrary[ContactDetails]
        cache <- arbitrary[CacheMap]
      } yield {
        new FakeContactDetailsRequiredAction(cache, details)
      }
    }

  private val fakeContactDetailsRequiredAction = arbitraryFakeContactDetailsActions.arbitrary.retryUntil(_ => true).sample.get
  private val mockCustomsDeclarationsService = mock[CustomsDeclarationsService]
  private val mockWSResponse = mock[WSResponse]

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockCustomsDeclarationsService, mockWSResponse)
    when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(FileUploadResponse(List())))
  }

  private def controller(contactDetailsRequiredAction: ContactDetailsRequiredAction) =
    new HowManyFilesUploadController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      new FakeDataRetrievalAction(None),
      new MrnRequiredAction,
      contactDetailsRequiredAction,
      new FileUploadCountProvider,
      mockDataCacheConnector,
      mockUploadContactDetails,
      mockCustomsDeclarationsService,
      appConfig)

  "How Many Files Upload Page" must {

    "load correct page when user is logged in " in {

      forAll { action: ContactDetailsRequiredAction =>

        val result = controller(action).onPageLoad(fakeRequest)

        status(result) mustBe OK
        val doc = asDocument(contentAsString(result))
        doc.title mustEqual "How many files do you need to upload?"
      }
    }

    "display file count if it exist in the cache" in {
      val updatedCacheMap = fakeContactDetailsRequiredAction.cacheMap.copy(data = fakeContactDetailsRequiredAction.cacheMap.data + (HowManyFilesUploadPage.toString -> JsNumber(7)))
      val updatedAction = new FakeContactDetailsRequiredAction(updatedCacheMap, fakeContactDetailsRequiredAction.contactDetails)

      val result = controller(updatedAction).onPageLoad(fakeRequest)

      val doc = asDocument(contentAsString(result))
      doc.select("#value").`val` mustEqual "7"
    }

    "redirect to error page when no data is found in the cache on page load" in {

      forAll { contactDetails: ContactDetails =>

        val action = new FakeContactDetailsRequiredAction(CacheMap("", Map()), contactDetails)
        val result = controller(action).onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
      }
    }

    "redirect to error page when no data is found in the cache on submit" in {

      forAll { contactDetails: ContactDetails =>

        val action = new FakeContactDetailsRequiredAction(CacheMap("", Map()), contactDetails)
        val result = controller(action).onSubmit(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
      }
    }

    "return a bad request when empty data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "")
      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)
      status(result) mustBe BAD_REQUEST
    }

    "return an ok and save to the data cache when valid data is submitted" in {
      val fileUploadResponse = FileUploadResponse(List(
        FileUpload("someFileRef1", Waiting(UploadRequest("http://s3bucket/myfile1", Map("" -> "")))),
        FileUpload("someFileRef2", Waiting(UploadRequest("http://s3bucket/myfile2", Map("" -> "")))),
        FileUpload("someFileRef3", Waiting(UploadRequest("http://s3bucket/myfile3", Map("" -> ""))))
      ))
      val fileUploadsAfterContactDetails = fileUploadResponse.files.tail

      when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(fileUploadResponse))
      when(mockUploadContactDetails.upload(any(), any())).thenReturn(Success(202))
      when(mockDataCacheConnector.save(any())(any[HeaderCarrier])).thenReturn(Future.successful(CacheMap("", Map.empty)))
      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2")

      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      val nextRef = fileUploadsAfterContactDetails.map(_.reference).min
      redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(nextRef).url)
      val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
      verify(mockDataCacheConnector).save(captor.capture())(any[HeaderCarrier])
      val Some(fileUploadCount) = FileUploadCount(2)
      captor.getValue.getEntry[FileUploadCount](HowManyFilesUploadPage) mustBe Some(fileUploadCount)
      captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response) mustBe Some(FileUploadResponse(fileUploadResponse.files.tail))
    }

    "redirect to error page when contact details upload fails" in {
      val fileUploadResponse = FileUploadResponse(List(
        FileUpload("someFileRef1", Waiting(UploadRequest("http://s3bucket/myfile1", Map("" -> "")))),
        FileUpload("someFileRef2", Waiting(UploadRequest("http://s3bucket/myfile2", Map("" -> ""))))
      ))
      reset(mockUploadContactDetails)
      when(mockCustomsDeclarationsService.batchFileUpload(any(), any(), any())(any())).thenReturn(Future.successful(fileUploadResponse))
      when(mockUploadContactDetails.upload(any(), any())).thenReturn(Failure(new IllegalStateException()))

      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2")

      val result = controller(fakeContactDetailsRequiredAction).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/cds-file-upload-service/error")
    }

    "make a request to customs declarations" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> "2")
      await(controller(fakeContactDetailsRequiredAction).onSubmit(postRequest))

      val Some(fileUploadCount) = FileUploadCount(2)
      verify(mockCustomsDeclarationsService).batchFileUpload(any(), eqTo(fakeContactDetailsRequiredAction.cacheMap.getEntry[MRN](MrnEntryPage).get), eqTo(fileUploadCount))(any())
    }
  }
}
