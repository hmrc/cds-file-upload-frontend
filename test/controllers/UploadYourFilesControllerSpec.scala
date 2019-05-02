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

import akka.stream.Materializer
import connectors.UpscanS3Connector
import controllers.actions.{DataRetrievalAction, FileUploadResponseRequiredAction}
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import pages.{ContactDetailsPage, HowManyFilesUploadPage, MrnEntryPage}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Flash, MaxSizeExceeded, MultipartFormData}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import views.html.upload_your_files

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}


class UploadYourFilesControllerSpec extends ControllerSpecBase {

  private val mockMaterializer: Materializer = mock[Materializer]
  private val mockUpscanConnector: UpscanS3Connector = mock[UpscanS3Connector]
  private val mockAuditConnector = mock[AuditConnector]

  private val responseGen: Gen[(FileUpload, FileUploadResponse)] =
    for {
      response <- arbitrary[FileUploadResponse]
      index <- Gen.choose(0, response.files.length - 1)
      file = response.files(index)
    } yield (file, response)

  private val waitingGen: Gen[(FileUpload, UploadRequest, FileUploadResponse)] = responseGen.flatMap {
    case (file, response) =>
      arbitrary[Waiting].map { waiting =>
        val uploadedFile = file.copy(state = waiting)
        val updatedFiles = uploadedFile :: response.files.filterNot(_ == file)

        (uploadedFile, waiting.uploadRequest, FileUploadResponse(updatedFiles))
      }
  }

  override def beforeEach = {
    super.beforeEach
    reset(mockMaterializer, mockUpscanConnector, mockAuditConnector)
  }

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction("GB987654321012"),
      getData,
      new FileUploadResponseRequiredAction(),
      mockDataCacheConnector,
      mockUpscanConnector,
      mockAuditConnector,
      appConfig,
      mockMaterializer)

  ".onPageLoad" should {

    "load the view" when {

      def nextPosition(ref: String, refs: List[String]): Position = {
        refs.indexOf(ref) match {
          case 0 => First(refs.size)
          case x if x == (refs.length - 1) => Last(refs.size)
          case x => Middle(x + 1, refs.size)
        }
      }

      "request file exists in response" in {

        forAll(waitingGen, arbitrary[CacheMap]) {
          case ((file, _, response), cacheMap) =>

            val refPosition: Position =
              nextPosition(file.reference, response.files.map(_.reference))

            val updatedCache = combine(response, cacheMap)
            val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe OK
            contentAsString(result) mustBe viewAsString(file.reference, refPosition, fakeRequest.flash)
        }
      }
    }

    "redirect to the next page" when {

      "file has already been uploaded" in {

        val fileUploadedGen = responseGen.map {
          case (file, _) =>
            val uploadedFile = file.copy(state = Uploaded)

            (uploadedFile, FileUploadResponse(List(uploadedFile)))
        }

        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, response), cache) =>

            val reference = nextRef(file.reference, response.files.map(_.reference))
            val nextPage = routes.UploadYourFilesController.onPageLoad(reference)
            val updatedCache = combine(response, cache)

            val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(nextPage.url)
        }
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(new FakeDataRetrievalAction(None)).onPageLoad(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>

          whenever(!response.files.exists(_.reference == ref)) {

            val updatedCache = combine(response, cache)
            val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }

  ".onSubmit" should {

    "upload to upscan, save the filename and redirect to next page when file is valid" in {

      val upscanRequest = UploadRequest("href", Map())
      val fileUploadedGen = responseGen.map {
        case (file, _) =>
          val uploadedFile = file.copy(state = Waiting(upscanRequest))
          (uploadedFile, FileUploadResponse(List(uploadedFile)))
      }

      forAll(fileUploadedGen, arbitrary[CacheMap]) {
        case ((file, response), cache) =>
          reset(mockUpscanConnector)
          reset(mockDataCacheConnector)
          when(mockDataCacheConnector.save(any[CacheMap])(any[HeaderCarrier])).thenReturn(Future.successful(CacheMap("", Map())))
          when(mockUpscanConnector.upload(any[UploadRequest], any[TemporaryFile], any[String])).thenReturn(Future.successful(()))

          val nextPage = routes.UploadYourFilesController.onSuccess(file.reference)
          val updatedCache = combine(response, cache)

          val fileName = "file.txt"
          val tempFile = TemporaryFile(fileName)

          val filePart = FilePart[TemporaryFile](key = "file", fileName, contentType = None, ref = tempFile)
          val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

          val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(file.reference)(fakeRequest.withBody(Right(form)))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(nextPage.url)

          verify(mockUpscanConnector).upload(refEq(upscanRequest), refEq(tempFile), eqTo(fileName))

          val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
          verify(mockDataCacheConnector).save(captor.capture())(any[HeaderCarrier])
          val answers = captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response)
          answers.get.files.head.filename mustBe fileName
      }
    }

    "redirect to the previous page" when {
      val upscanRequest = UploadRequest("href", Map())
      val fileUploadedGen = responseGen.map {
        case (file, _) =>
          val uploadedFile = file.copy(state = Waiting(upscanRequest))
          (uploadedFile, FileUploadResponse(List(uploadedFile)))
      }

      "file is Missing" in {
        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, response), cache) =>
            val updatedCache = combine(response, cache)

            val filePart = FilePart[TemporaryFile](key = "file", "", contentType = None, ref = TemporaryFile())
            val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

            val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(file.reference)(fakeRequest.withBody(Right(form)))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(file.reference).url)
        }
      }

      "file is too large" in {

        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, response), cache) =>
            val updatedCache = combine(response, cache)

            val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(file.reference)(fakeRequest.withBody(Left(MaxSizeExceeded(0))))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(file.reference).url)
        }
      }
    }


    "redirect to the next page" when {

      "file has already been uploaded" in {

        val fileUploadedGen = responseGen.map {
          case (file, _) =>
            val uploadedFile = file.copy(state = Uploaded)

            (uploadedFile, FileUploadResponse(List(uploadedFile)))
        }

        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, response), cache) =>

            val reference = nextRef(file.reference, response.files.map(_.reference))
            val nextPage = routes.UploadYourFilesController.onSubmit(reference)
            val updatedCache = combine(response, cache)

            val filePart = FilePart[TemporaryFile](key = "file", "file.txt", contentType = None, ref = TemporaryFile("file.txt"))
            val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

            val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(file.reference)(fakeRequest.withBody(Right(form)))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(nextPage.url)
        }
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        val filePart = FilePart[TemporaryFile](key = "file", "file.txt", contentType = None, ref = TemporaryFile("file.txt"))
        val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

        val result = controller(new FakeDataRetrievalAction(None)).onSubmit("someRef")(fakeRequest.withBody(Right(form)))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
      }

      "file reference is not in response" in {

        forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>

          whenever(!response.files.exists(_.reference == ref)) {

            val filePart = FilePart[TemporaryFile](key = "file", "file.txt", contentType = None, ref = TemporaryFile("file.txt"))
            val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

            val updatedCache = combine(response, cache)
            val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(ref)(fakeRequest.withBody(Right(form)))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }

  ".onSuccess" should {

    "update file status to Uploaded" in {

      forAll(waitingGen, arbitrary[CacheMap]) {
        case ((file, _, response), cache) =>

          val updatedCache = combine(response, cache)
          await(controller(fakeDataRetrievalAction(updatedCache)).onSuccess(file.reference)(fakeRequest))

          val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
          verify(mockDataCacheConnector, atLeastOnce).save(captor.capture())(any[HeaderCarrier])

          val Some(updateResponse) = captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response)
          val Some(updatedFile) = updateResponse.files.find(_.reference == file.reference)
          updatedFile.state mustBe Uploaded
      }
    }

    "redirect user to the next upload page" in {

      forAll(waitingGen, arbitrary[CacheMap]) {
        case ((file, _, response), cache: CacheMap) =>

          val updatedCache = combine(response, cache)
          val result = controller(fakeDataRetrievalAction(updatedCache)).onSuccess(file.reference)(fakeRequest)
          val next = nextRef(file.reference, response.files.collect { case FileUpload(reference, Waiting(_), _) => reference })

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(next).url)
      }
    }

    "audit upload success" in {

      val file1 = FileUpload("fileRef1", Waiting(UploadRequest("some href", Map.empty)))
      val file2 = FileUpload("fileRef2", Waiting(UploadRequest("some other href", Map.empty)))
      val lastFile = FileUpload("fileRef3", Waiting(UploadRequest("another href", Map.empty)))
      val response = FileUploadResponse(List(file1, file2, lastFile))

      val Some(mrn) = MRN("34GB1234567ABCDEFG")
      val cd = ContactDetails("someNicky", "toNicky", "0123456789", "ntn@nicky.nz")
      val cache = CacheMap("someId", Map(
        MrnEntryPage.toString -> Json.toJson(mrn),
        HowManyFilesUploadPage.toString -> Json.toJson(FileUploadCount(3)),
        ContactDetailsPage.toString -> Json.toJson(cd)
      ))
      val updatedCache = combine(response, cache)

      val expectedDetail = Map(
        "eori" -> "GB987654321012",
        "fullName" -> cd.name,
        "companyName" -> cd.companyName,
        "emailAddress" -> cd.email,
        "telephoneNumber" -> cd.phoneNumber,
        "mrn" -> mrn.value,
        "numberOfFiles" -> "3"
      ) ++ referencesMap(response.files)

      val result = controller(fakeDataRetrievalAction(updatedCache)).onSuccess(lastFile.reference)(fakeRequest)
      status(result) mustBe SEE_OTHER

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockAuditConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])

      val dataEvent = captor.getValue
      dataEvent.auditType mustBe "UploadSuccess"
      dataEvent.auditSource mustBe "cds-file-upload-frontend"
      dataEvent.detail mustBe expectedDetail
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(new FakeDataRetrievalAction(None)).onSuccess(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>

          whenever(!response.files.exists(_.reference == ref)) {

            val updatedCache = combine(response, cache)
            val result = controller(fakeDataRetrievalAction(updatedCache)).onSuccess(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }

  private def viewAsString(reference: String, refPosition: Position, flash: Flash) = upload_your_files(reference, refPosition)(fakeRequest, messages, appConfig, flash).toString

  private def combine(response: FileUploadResponse, cache: CacheMap) =
    cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))

  private def nextRef(ref: String, refs: List[String]) = {
    val index = refs.sorted.indexOf(ref)
    refs.sorted.drop(index + 1).headOption.getOrElse("receipt")
  }

  private def referencesMap(files: List[FileUpload]): Map[String, String] = ListMap((1 to files.size).map(i => "fileReference").zip(files.map(_.reference)): _*)
}
