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
import controllers.actions.{DataRetrievalAction, FakeActions, FileUploadResponseRequiredAction}
import generators.Generators
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.HowManyFilesUploadPage
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MaxSizeExceeded, MultipartFormData}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.upload_your_files

import scala.concurrent.Future


class UploadYourFilesControllerSpec extends ControllerSpecBase
  with ScalaFutures
  with MockitoSugar
  with PropertyChecks
  with Generators
  with FakeActions {

  val materializer: Materializer = mock[Materializer]
  val upscanConnector: UpscanS3Connector = mock[UpscanS3Connector]

  val responseGen: Gen[(File, FileUploadResponse)] =
    for {
      response <- arbitrary[FileUploadResponse]
      index    <- Gen.choose(0, response.files.length - 1)
      file      = response.files(index)
    } yield (file, response)

  val waitingGen: Gen[(File, UploadRequest, FileUploadResponse)] =
    responseGen.flatMap {
      case (file, response) =>
        arbitrary[Waiting].map { waiting =>
          val uploadedFile = file.copy(state = waiting)
          val updatedFiles = uploadedFile :: response.files.filterNot(_ == file)

          (uploadedFile, waiting.uploadRequest, FileUploadResponse(updatedFiles))
        }
    }

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      getData,
      new FileUploadResponseRequiredAction(),
      mockDataCacheConnector,
      upscanConnector,
      appConfig,
      materializer)

  def viewAsString(reference: String, refPosition: Position): String =
    upload_your_files(reference, refPosition)(fakeRequest, messages, appConfig).toString

  private def combine(response: FileUploadResponse, cache: CacheMap): CacheMap =
    cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))

  private def nextRef(ref: String, refs: List[String]): String = {
    val index = refs.sorted.indexOf(ref)
    refs.sorted.drop(index + 1).headOption.getOrElse("receipt")
  }

  ".onPageLoad" should {

    "load the view" when {

      def nextPosition(ref: String, refs: List[String]): Position = {
        refs.indexOf(ref) match {
          case 0                           => First(refs.size)
          case x if x == (refs.length - 1) => Last(refs.size)
          case x                           => Middle(x + 1, refs.size)
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
            contentAsString(result) mustBe viewAsString(file.reference, refPosition)
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

            val reference    = nextRef(file.reference, response.files.map(_.reference))
            val nextPage     = routes.UploadYourFilesController.onPageLoad(reference)
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

    "upload to upscan & redirect to next page" when {

      val upscanRequest = UploadRequest("href", Map())
      val fileUploadedGen = responseGen.map {
        case (file, _) =>
          val uploadedFile = file.copy(state = Waiting(upscanRequest))

          (uploadedFile, FileUploadResponse(List(uploadedFile)))
      }

      "file is valid" in {

        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, response), cache) =>
            reset(upscanConnector)
            given(upscanConnector.upload(any[UploadRequest], any[TemporaryFile])) willReturn Future.successful((): Unit)

            val nextPage     = routes.UploadYourFilesController.onSuccess(file.reference)
            val updatedCache = combine(response, cache)


            val filePart = FilePart[TemporaryFile](key = "file", "file.txt", contentType = None, ref = TemporaryFile("file.txt"))
            val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

            val result = controller(fakeDataRetrievalAction(updatedCache)).onSubmit(file.reference)(fakeRequest.withBody(Right(form)))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(nextPage.url)

            verify(upscanConnector).upload(refEq(upscanRequest), any[TemporaryFile])
        }
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

            val reference    = nextRef(file.reference, response.files.map(_.reference))
            val nextPage     = routes.UploadYourFilesController.onSubmit(reference)
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

        forAll { ref: String =>

          val filePart = FilePart[TemporaryFile](key = "file", "file.txt", contentType = None, ref = TemporaryFile("file.txt"))
          val form = MultipartFormData[TemporaryFile](dataParts = Map(), files = Seq(filePart), badParts = Seq.empty)

          val result = controller(new FakeDataRetrievalAction(None)).onSubmit(ref)(fakeRequest.withBody(Right(form)))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
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
          val result = controller(fakeDataRetrievalAction(updatedCache)).onSuccess(file.reference)(fakeRequest)

          whenReady(result) { _ =>

            val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
            verify(mockDataCacheConnector, atLeastOnce).save(captor.capture())(any[HeaderCarrier])

            val updateResponse = captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response)

            updateResponse must not be Some(response)
            updateResponse
              .flatMap(_.files.find(_.reference == file.reference))
              .map(_.state) mustBe Some(Uploaded)
          }
      }
    }

    "redirect user to the next upload page" in {

      forAll(waitingGen, arbitrary[CacheMap]) {
        case ((file, _, response), cache: CacheMap) =>

          val updatedCache = combine(response, cache)
          val result = controller(fakeDataRetrievalAction(updatedCache)).onSuccess(file.reference)(fakeRequest)
          val next = nextRef(file.reference, response.files.collect { case file@File(_, Waiting(_)) => file.reference })

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(next).url)
      }
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
}
