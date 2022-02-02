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

import base.SfusMetricsMock
import config.Notifications
import connectors.CdsFileUploadConnector
import controllers.actions.{DataRetrievalAction, FileUploadResponseRequiredAction}
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.AuditService
import testdata.CommonTestData.{eori, signedInUser}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{upload_error, upload_your_files}

import scala.concurrent.Future

class UpscanStatusControllerSpec extends ControllerSpecBase with SfusMetricsMock {

  private val auditService = mock[AuditService]
  private val uploadYourFiles = mock[upload_your_files]
  private val uploadError = mock[upload_error]
  private val cdsFileUploadConnector = mock[CdsFileUploadConnector]

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

  def controller(getData: DataRetrievalAction = fakeDataRetrievalAction()) =
    new UpscanStatusController(
      new FakeAuthAction(signedInUser),
      getData,
      new FakeMrnRequiredAction(),
      new FakeVerifiedEmailAction(),
      new FileUploadResponseRequiredAction(),
      mockFileUploadAnswersService,
      auditService,
      cdsFileUploadConnector,
      appConfig.copy(notifications = Notifications(maxRetries = 3, retryPauseMillis = 500)),
      mcc,
      sfusMetrics,
      uploadYourFiles,
      uploadError
    )(executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(uploadYourFiles.apply(any(), any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)
    when(uploadError.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(auditService, cdsFileUploadConnector, uploadYourFiles, uploadError, mockFileUploadAnswersService)

    super.afterEach()
  }

  "Upscan Status error" should {

    "return error page" in {
      val result = controller().error()(fakeRequest)

      status(result) mustBe OK
      verify(uploadError).apply()(any(), any())
    }
  }

  ".onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll(waitingGen) {
          case (file, _, response) =>
            val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))
            val result = controller(fakeDataRetrievalAction(answers)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe OK
        }
      }
    }

    "redirect to the next page" when {

      "file upload is in an 'Uploaded' state" in {
        val fileUpload = FileUpload("ref1", Uploaded, id = "ref1")
        val response = FileUploadResponse(List(fileUpload))

        val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))

        when(cdsFileUploadConnector.getNotification(any())(any()))
          .thenReturn(Future.successful(Some(Notification("ref1", "SUCCESS", Some("file1.pdf")))))
        val result = controller(fakeDataRetrievalAction(answers)).onPageLoad(fileUpload.reference)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UploadYourFilesReceiptController.onPageLoad.url)
      }
    }
  }

  ".success" should {
    val fileUpload1 = FileUpload("ref1", Uploaded, id = "ref1")
    val fileUpload2 = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
    val response = FileUploadResponse(List(fileUpload1, fileUpload2))

    "redirect to next page when file is valid" in {
      val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))

      val result = controller(fakeDataRetrievalAction(answers)).success(fileUpload1.id)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad(fileUpload2.id).url)
    }

    "file upload is in an 'Uploaded' state" in {
      val fileUpload1 = FileUpload("ref1", Uploaded, id = "ref1")
      val fileUpload2 = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
      val response = FileUploadResponse(List(fileUpload1, fileUpload2))

      when(cdsFileUploadConnector.getNotification(any())(any()))
        .thenReturn(Future.successful(Some(Notification("ref1", "SUCCESS", Some("myfile.doc")))))
      val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))

      val result = controller(fakeDataRetrievalAction(answers)).success("ref1")(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad("ref2").url)
    }
  }

  "redirect to error page" when {

    "no responses are in the cache" in {

      val result = controller(new FakeDataRetrievalAction(None)).success("someRef")(fakeRequest)

      result.map(_ => verify(mockFileUploadAnswersService).remove(any()))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.error.url)
    }

    "file reference is not in response" in {

      forAll { (ref: String, response: FileUploadResponse) =>
        whenever(!response.uploads.exists(_.reference == ref)) {

          val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))
          val result = controller(fakeDataRetrievalAction(answers)).success(ref)(fakeRequest)

          result.map(_ => verify(mockFileUploadAnswersService).remove(any()))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ErrorPageController.error.url)
        }
      }
    }
  }

  ".success" should {

    "update file status to Uploaded" in {

      val fileUpload = FileUpload("ref", Waiting(UploadRequest("href", Map.empty)), id = "ref")
      val response = FileUploadResponse(List(fileUpload))

      when(cdsFileUploadConnector.getNotification(any())(any()))
        .thenReturn(Future.successful(Some(Notification("ref", "SUCCESS", Some("myfile.pdf")))))

      val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))
      await(controller(fakeDataRetrievalAction(answers)).success("ref")(fakeRequest))

      val Some(updateResponse) = theSavedFileUploadAnswers.fileUploadResponse
      val Some(updatedFile) = updateResponse.uploads.find(_.id == "ref")
      updatedFile.state mustBe Uploaded
    }

    "redirect user to the next upload page" in {

      val fileUploaded = FileUpload("ref1", Uploaded, id = "ref1")
      val fileUploadWaiting = FileUpload("ref2", Waiting(UploadRequest("href", Map.empty)), id = "ref2")
      val response = FileUploadResponse(List(fileUploaded, fileUploadWaiting))

      val answers = FileUploadAnswers(eori, fileUploadResponse = Some(response))
      val result = controller(fakeDataRetrievalAction(answers)).success(fileUploaded.reference)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpscanStatusController.onPageLoad(fileUploadWaiting.reference).url)
    }

    "audit upload success" in {
      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456")
      val answers =
        FileUploadAnswers(eori, contactDetails = Some(cd), mrn = Some(mrn), fileUploadCount = FileUploadCount(3), fileUploadResponse = Some(response))

      when(cdsFileUploadConnector.getNotification(meq("fileRef1"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef1", "SUCCESS", Some("file1.pdf")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef2"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef2", "SUCCESS", Some("file2.doc")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef3"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef3", "SUCCESS", Some("file3.png")))))

      val result = controller(fakeDataRetrievalAction(answers)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER

      verify(auditService).auditUploadSuccess(meq(eori), meq(Some(cd)), meq(Some(mrn)), meq(FileUploadCount(3)), meq(response.uploads))(
        any[HeaderCarrier]
      )
    }

    "load receipt page when all notifications are successful" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456")
      val answers =
        FileUploadAnswers(eori, contactDetails = Some(cd), mrn = Some(mrn), fileUploadCount = FileUploadCount(3), fileUploadResponse = Some(response))

      when(cdsFileUploadConnector.getNotification(meq("fileRef1"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef1", "SUCCESS", Some("file1.pdf")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef2"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef2", "SUCCESS", Some("file2.doc")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef3"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef3", "SUCCESS", Some("file3.gif")))))

      val result = controller(fakeDataRetrievalAction(answers)).success(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UploadYourFilesReceiptController.onPageLoad.url)
    }

    "load upload error page when we get a fail notification" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456")
      val answers =
        FileUploadAnswers(eori, contactDetails = Some(cd), mrn = Some(mrn), fileUploadCount = FileUploadCount(3), fileUploadResponse = Some(response))

      when(cdsFileUploadConnector.getNotification(meq("fileRef1"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef1", "SUCCESS", Some("file1.pdf")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef2"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef2", "FAIL", Some("file2.doc")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef3"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef3", "SUCCESS", Some("file3.gif")))))

      val result = controller(fakeDataRetrievalAction(answers)).success(lastFile.reference)(fakeRequest)
      result.map(_ => verify(mockFileUploadAnswersService).remove(any()))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.uploadError.url)
    }

    "load upload error page when notification retries are exceeded" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)), id = "fileRef1")
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)), id = "fileRef2")
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)), id = "fileRef3")
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("Joe Bloggs", "Bloggs Inc", "07998123456")
      val answers =
        FileUploadAnswers(eori, contactDetails = Some(cd), mrn = Some(mrn), fileUploadCount = FileUploadCount(3), fileUploadResponse = Some(response))

      when(cdsFileUploadConnector.getNotification(meq("fileRef1"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef1", "SUCCESS", Some("file1.pdf")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef2"))(any()))
        .thenReturn(Future.successful(Some(Notification("fileRef2", "SUCCESS", Some("file2.doc")))))
      when(cdsFileUploadConnector.getNotification(meq("fileRef3"))(any()))
        .thenReturn(Future.successful(None))

      val result = controller(fakeDataRetrievalAction(answers)).success(lastFile.reference)(fakeRequest)
      result.map(_ => verify(mockFileUploadAnswersService).remove(any()))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ErrorPageController.uploadError.url)
    }
  }
}
