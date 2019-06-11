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
import models.{FileUpload, FileUploadResponse, Notification}
import pages.HowManyFilesUploadPage
import play.api.libs.json.{JsString, Json}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.NotificationRepository

import scala.concurrent.{ExecutionContext, Future}

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase {
  
  implicit val ac = appConfig
  val mockNotificationRepository = mock[NotificationRepository]
  
  def controller(getData: DataRetrievalAction) = new UploadYourFilesReceiptController(messagesApi, new FakeAuthAction(), new FakeEORIAction(), getData, new FileUploadResponseRequiredAction(), mockNotificationRepository)

  def viewAsString(receipts: List[FileUpload]): String = views.html.upload_your_files_receipt(receipts)(fakeRequest, messages, appConfig).toString

  def addFilenames(uploads: List[FileUpload]): List[FileUpload] = uploads.map(u => u.copy(filename = "someFile.pdf"))

  "onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll { (response: FileUploadResponse, cache: CacheMap) =>
          response.uploads.foreach { u =>
            when(mockNotificationRepository.find(any())(any[ExecutionContext])).thenReturn(Future.successful(List(Notification(u.reference, "SUCCESS", "someFile.pdf"))))
          }

          val updatedCache = cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))
          val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad()(fakeRequest)

          status(result) mustBe OK
          contentAsString(result) mustBe viewAsString(addFilenames(response.uploads.sortBy(_.reference)))
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
