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

import controllers.actions.{DataRetrievalAction, FakeActions, FileUploadResponseRequiredActionImpl}
import generators.Generators
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.HowManyFilesUploadPage
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.upload_your_files


class UploadYourFilesControllerSpec extends ControllerSpecBase
  with ScalaFutures
  with MockitoSugar
  with PropertyChecks
  with Generators
  with FakeActions {

  val responseGen: Gen[(File, FileUploadResponse)] =
    for {
      response <- arbitrary[FileUploadResponse]
      index    <- Gen.choose(0, response.files.length - 1)
      file      = response.files(index)
    } yield (file, response)

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      getData,
      new FileUploadResponseRequiredActionImpl(),
      dataCacheConnector,
      appConfig)

  def viewAsString(uploadRequest: UploadRequest, callbackUrl: String, refPosition: Position): String =
    upload_your_files(uploadRequest, callbackUrl, refPosition)(fakeRequest, messages, appConfig).toString

  private def combine(response: FileUploadResponse, cache: CacheMap): CacheMap =
    cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))

  ".onPageLoad" should {

    "load the view" when {

      def nextRef(ref: String, refs: List[String]): String = {
        val index = refs.sorted.indexOf(ref)
        refs.sorted.drop(index + 1).headOption.getOrElse("receipt")
      }

      def nextPosition(ref: String, refs: List[String]): Position = {
        refs.indexOf(ref) match {
          case 0                           => First
          case x if x == (refs.length - 1) => Last
          case _                           => Middle
        }
      }

      "request file exists in response" in {

        forAll(responseGen, arbitrary[CacheMap]) {
          case ((file, response), cacheMap) =>

            val callback =
              routes.UploadYourFilesController.onSuccess(file.reference).absoluteURL()(fakeRequest)

            val refPosition: Position =
              nextPosition(file.reference, response.files.map(_.reference))

            val updatedCache = combine(response, cacheMap)
            val result = controller(getCacheMap(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe OK
            contentAsString(result) mustBe viewAsString(file.uploadRequest, callback, refPosition)
        }
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(getEmptyCacheMap).onPageLoad(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>

          whenever(!response.files.exists(_.reference == ref)) {

            val updatedCache = combine(response, cache)
            val result = controller(getCacheMap(updatedCache)).onPageLoad(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }

  ".onSuccess" should {

    def nextRef(ref: String, refs: List[String]): String = {
      val index = refs.sorted.indexOf(ref)
      refs.sorted.drop(index + 1).headOption.getOrElse("receipt")
    }

    "update file status to Uploaded" in {

      forAll(responseGen, arbitrary[CacheMap]) {
        case ((file, response), cache: CacheMap) =>

          val updatedCache = combine(response, cache)
          val result = controller(getCacheMap(updatedCache)).onSuccess(file.reference)(fakeRequest)

          whenReady(result) { _ =>
            val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
            verify(dataCacheConnector, atLeastOnce).save(captor.capture())

            val updateResponse = captor.getValue.getEntry[FileUploadResponse](HowManyFilesUploadPage.Response)

            updateResponse must not be Some(response)
            updateResponse
              .flatMap(_.files.find(_.reference == file.reference))
              .map(_.state) mustBe Some(Uploaded)
          }
      }
    }

    "redirect user to the next upload page" in {

      forAll(responseGen, arbitrary[CacheMap]) {
        case ((file, response), cache: CacheMap) =>

          val updatedCache = combine(response, cache)
          val result = controller(getCacheMap(updatedCache)).onSuccess(file.reference)(fakeRequest)
          val next = nextRef(file.reference, response.files.map(_.reference))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(next).url)
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(getEmptyCacheMap).onSuccess(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, response: FileUploadResponse, cache: CacheMap) =>

          whenever(!response.files.exists(_.reference == ref)) {

            val updatedCache = combine(response, cache)
            val result = controller(getCacheMap(updatedCache)).onSuccess(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }
}
