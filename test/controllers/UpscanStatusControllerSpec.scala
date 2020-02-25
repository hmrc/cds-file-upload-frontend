/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import config.Notifications
import connectors.Cache
import controllers.actions.{DataRetrievalAction, FileUploadResponseRequiredAction}
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import pages.{ContactDetailsPage, HowManyFilesUploadPage, MrnEntryPage}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import repositories.NotificationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import views.html.{upload_error, upload_your_files}

import scala.concurrent.{ExecutionContext, Future}

class UpscanStatusControllerSpec extends ControllerSpecBase {

  val cache = mock[Cache]
  val notificationRepository = mock[NotificationRepository]
  val auditConnector = mock[AuditConnector]
  val uploadYourFiles = mock[upload_your_files]
  val uploadError = mock[upload_error]

  val controller = new UpscanStatusController(
    new FakeAuthAction(),
    new FakeEORIAction("GB987654321012"),
    fakeDataRetrievalAction(CacheMap("", Map.empty)),
    new FileUploadResponseRequiredAction(),
    cache,
    notificationRepository,
    auditConnector,
    appConfig,
    mcc,
    uploadYourFiles,
    uploadError
  )(executionContext)

  private val mockMaterializer = mock[Materializer]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockNotificationRepository = mock[NotificationRepository]

  private val responseGen: Gen[(FileUpload, FileUploadResponse)] =
    for {
      response <- arbitrary[FileUploadResponse]
      index <- Gen.choose(0, response.uploads.length - 1)
      upload = response.uploads(index)
    } yield (upload, response)

  private val waitingGen: Gen[(FileUpload, UploadRequest, FileUploadResponse)] = responseGen.flatMap {
    case (file, response) =>
      arbitrary[Waiting].map { waiting =>
        val uploadedFile = file.copy(state = waiting)
        val updatedFiles = uploadedFile :: response.uploads.filterNot(_ == file)

        (uploadedFile, waiting.uploadRequest, FileUploadResponse(updatedFiles))
      }
  }

  def controller(getData: DataRetrievalAction) =
    new UpscanStatusController(
      new FakeAuthAction(),
      new FakeEORIAction("GB987654321012"),
      getData,
      new FileUploadResponseRequiredAction(),
      mockDataCacheConnector,
      mockNotificationRepository,
      mockAuditConnector,
      appConfig.copy(notifications = Notifications(appConfig.notifications.authToken, maxRetries = 3, retryPauseMillis = 500, ttlSeconds = 60)),
      mcc,
      uploadYourFiles,
      uploadError
    )(executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(uploadYourFiles.apply(any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)
    when(uploadError.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockMaterializer, mockAuditConnector, mockNotificationRepository, uploadYourFiles, uploadError)

    super.afterEach()
  }

  "Upscan Status error" should {

    "return error page" in {
      val result = controller.error("someId")(fakeRequest)

      status(result) mustBe OK
      verify(uploadError).apply()(any(), any())
    }
  }

  ".onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll(waitingGen, arbitrary[CacheMap]) {
          case ((file, _, response), cacheMap) =>
            val updatedCache = combine(response, cacheMap)
            val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe OK
        }
      }
    }

    "redirect to the next page" when {

      "file upload is in an 'Uploaded' state" in {
        val fileUpload = FileUpload("ref1", Uploaded, id = "ref1")
        val response = FileUploadResponse(List(fileUpload))

        forAll { cache: CacheMap =>
          val updatedCache = combine(response, cache)

          when(mockNotificationRepository.find(any())(any[ExecutionContext]))
            .thenReturn(Future.successful(List(Notification("ref1", "SUCCESS", "file1.pdf"))))
          val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad(fileUpload.reference)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UploadYourFilesReceiptController.onPageLoad().url)
        }
      }
    }
  }

  ".success" should {
    val fileUpload1 = FileUpload("ref1", Uploaded, id = "ref1")
    val fileUpload2 = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
    val response = FileUploadResponse(List(fileUpload1, fileUpload2))

    "redirect to next page when file is valid" in {

      forAll { cache: CacheMap =>
        reset(mockDataCacheConnector)
        when(mockDataCacheConnector.save(any[CacheMap])(any[HeaderCarrier])).thenReturn(Future.successful(CacheMap("", Map())))

        val updatedCache = combine(response, cache)

        val result = controller(fakeDataRetrievalAction(updatedCache)).success(fileUpload1.id)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad(fileUpload2.id).url)
      }
    }

    "file upload is in an 'Uploaded' state" in {
      val fileUpload1 = FileUpload("ref1", Uploaded, id = "ref1")
      val fileUpload2 = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
      val response = FileUploadResponse(List(fileUpload1, fileUpload2))

      forAll { cache: CacheMap =>
        when(mockNotificationRepository.find(any())(any[ExecutionContext]))
          .thenReturn(Future.successful(List(Notification("ref1", "SUCCESS", "myfile.doc"))))
        val updatedCache = combine(response, cache)

        val result = controller(fakeDataRetrievalAction(updatedCache)).success("ref1")(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad("ref2").url)
      }
    }
  }

  "redirect to error page" when {

    "no responses are in the cache" in {

      val result = controller(new FakeDataRetrievalAction(None)).success("someRef")(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.error().url)
    }

    "file reference is not in response" in {

      forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>
        whenever(!response.uploads.exists(_.reference == ref)) {

          val updatedCache = combine(response, cache)
          val result = controller(fakeDataRetrievalAction(updatedCache)).success(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ErrorPageController.error().url)
        }
      }
    }
  }

  ".success" should {

    "update file status to Uploaded" in {

      val fileUpload = FileUpload("ref", Waiting(UploadRequest("href", Map.empty)), id = "ref")
      val response = FileUploadResponse(List(fileUpload))

      forAll { cache: CacheMap =>
        when(mockNotificationRepository.find(any())(any[ExecutionContext]))
          .thenReturn(Future.successful(List(Notification("ref", "SUCCESS", "myfile.xls"))))

        val updatedCache = combine(response, cache)
        await(controller(fakeDataRetrievalAction(updatedCache)).success("ref")(fakeRequest))

        val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
        verify(mockDataCacheConnector, atLeastOnce).save(captor.capture())(any[HeaderCarrier])

        val Some(updateResponse) = captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response)
        val Some(updatedFile) = updateResponse.uploads.find(_.id == "ref")
        updatedFile.state mustBe Uploaded
      }
    }

    "redirect user to the next upload page" in {

      val fileUploaded = FileUpload("ref1", Uploaded, id = "ref1")
      val fileUploadWaiting = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
      val response = FileUploadResponse(List(fileUploaded, fileUploadWaiting))

      forAll { cache: CacheMap =>
        val updatedCache = combine(response, cache)
        val result = controller(fakeDataRetrievalAction(updatedCache)).success(fileUploaded.reference)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad(fileUploadWaiting.reference).url)
      }
    }

    "audit upload success" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456", "joe@bloggs.com")
      val cache = CacheMap(
        "someId",
        Map(
          MrnEntryPage.toString -> Json.toJson(mrn),
          HowManyFilesUploadPage.toString -> Json.toJson(FileUploadCount(3)),
          ContactDetailsPage.toString -> Json.toJson(cd)
        )
      )
      val updatedCache = combine(response, cache)

      val expectedDetail = Map(
        "eori" -> "GB987654321012",
        "fullName" -> "Joe Bloggs",
        "companyName" -> "Bloggs Inc",
        "emailAddress" -> "joe@bloggs.com",
        "telephoneNumber" -> "07998123456",
        "mrn" -> "34GB1234567ABCDEFG",
        "numberOfFiles" -> "3",
        "fileReference1" -> "fileRef1",
        "fileReference2" -> "fileRef2",
        "fileReference3" -> "fileRef3"
      )

      when(mockNotificationRepository.find(any())(any[ExecutionContext])).thenReturn(
        Future.successful(List(Notification("fileRef1", "SUCCESS", "file1.pdf"))), //first find
        Future.successful(List(Notification("fileRef2", "SUCCESS", "file2.doc"))), //second find
        Future.successful(List(Notification("fileRef3", "SUCCESS", "file3.png"))) //third find
      )

      val result = controller(fakeDataRetrievalAction(updatedCache)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockAuditConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])

      val dataEvent = captor.getValue
      dataEvent.auditType mustBe "UploadSuccess"
      dataEvent.auditSource mustBe "cds-file-upload-frontend"
      dataEvent.detail mustBe expectedDetail
    }

    "load receipt page when all notifications are successful" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456", "joe@bloggs.com")
      val cache = CacheMap(
        "someId",
        Map(
          MrnEntryPage.toString -> Json.toJson(mrn),
          HowManyFilesUploadPage.toString -> Json.toJson(FileUploadCount(3)),
          ContactDetailsPage.toString -> Json.toJson(cd)
        )
      )
      val updatedCache = combine(response, cache)

      when(mockNotificationRepository.find(any())(any[ExecutionContext])).thenReturn(
        Future.successful(List(Notification("fileRef1", "SUCCESS", "file1.pdf"))), //first find
        Future.successful(List(Notification("fileRef2", "SUCCESS", "file2.doc"))), //second find
        Future.successful(List(Notification("fileRef3", "SUCCESS", "file3.gif"))) //third find
      )

      val result = controller(fakeDataRetrievalAction(updatedCache)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UploadYourFilesReceiptController.onPageLoad().url)
    }

    "load upload error page when we get a fail notification" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456", "joe@bloggs.com")
      val cache = CacheMap(
        "someId",
        Map(
          MrnEntryPage.toString -> Json.toJson(mrn),
          HowManyFilesUploadPage.toString -> Json.toJson(FileUploadCount(3)),
          ContactDetailsPage.toString -> Json.toJson(cd)
        )
      )
      val updatedCache = combine(response, cache)

      when(mockNotificationRepository.find(any())(any[ExecutionContext])).thenReturn(
        Future.successful(List(Notification("fileRef1", "SUCCESS", "file1.pdf"))), //first find
        Future.successful(List(Notification("fileRef2", "FAIL", "file2.doc"))), //second find
        Future.successful(List(Notification("fileRef3", "SUCCESS", "file3.gif"))) //third find
      )

      val result = controller(fakeDataRetrievalAction(updatedCache)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.uploadError().url)
    }

    "load upload error page when notification retries are exceeded" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456", "joe@bloggs.com")
      val cache = CacheMap(
        "someId",
        Map(
          MrnEntryPage.toString -> Json.toJson(mrn),
          HowManyFilesUploadPage.toString -> Json.toJson(FileUploadCount(3)),
          ContactDetailsPage.toString -> Json.toJson(cd)
        )
      )
      val updatedCache = combine(response, cache)

      when(mockNotificationRepository.find(any())(any[ExecutionContext])).thenReturn(
        Future.successful(List(Notification("fileRef1", "SUCCESS", "file1.doc"))), //first find
        Future.successful(List(Notification("fileRef2", "SUCCESS", "file2.xls"))), //second find
        Future.successful(List.empty) //third find
      )

      val result = controller(fakeDataRetrievalAction(updatedCache)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.uploadError().url)
    }

  }

  private def combine(response: FileUploadResponse, cache: CacheMap): CacheMap =
    cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))
}
