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
import models.{File, FileUploadResponse, UploadRequest}
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import pages.HowManyFilesUploadPage
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.upload_your_files_receipt

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase with PropertyChecks with Generators with FakeActions {

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesReceiptController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      getData,
      new FileUploadResponseRequiredActionImpl(),
      appConfig)

  def viewAsString(receipts: List[String]): String =
    upload_your_files_receipt(receipts)(fakeRequest, messages, appConfig).toString

  ".onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll { (response: FileUploadResponse, cache: CacheMap) =>

          val updatedCache = cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))
          val result = controller(getCacheMap(updatedCache)).onPageLoad()(fakeRequest)

          status(result) mustBe OK
          contentAsString(result) mustBe viewAsString(response.files.map(_.reference).sorted)
        }
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        val result = controller(getEmptyCacheMap).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
      }
    }
  }
}
