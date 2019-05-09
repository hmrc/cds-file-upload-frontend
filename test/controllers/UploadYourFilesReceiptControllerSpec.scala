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

import controllers.actions.{DataRetrievalAction, FileUploadResponseRequiredAction}
import models.{FileUpload, FileUploadResponse}
import pages.HowManyFilesUploadPage
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase {

  def controller(getData: DataRetrievalAction) = new UploadYourFilesReceiptController(messagesApi, new FakeAuthAction(), new FakeEORIAction(), getData, new FileUploadResponseRequiredAction(), appConfig)

  def viewAsString(receipts: List[FileUpload]): String = views.html.upload_your_files_receipt(receipts)(fakeRequest, messages, appConfig).toString

  "onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll { (response: FileUploadResponse, cache: CacheMap) =>

          val updatedCache = cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))
          val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad()(fakeRequest)

          status(result) mustBe OK
          contentAsString(result) mustBe viewAsString(response.files.sortBy(_.reference))
        }
      }
    }

    "redirect to error page" when {

      "no responses are in the cache" in {

        val result = controller(new FakeDataRetrievalAction(None)).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ErrorPageController.error().url)
      }
    }
  }
}
