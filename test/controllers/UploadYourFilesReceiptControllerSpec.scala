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

import connectors.CdsFileUploadConnector
import controllers.actions.{DataRetrievalAction, FileUploadResponseRequiredAction}
import models.{FileUpload, FileUploadResponse, Notification}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import pages.HowManyFilesUploadPage
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.upload_your_files_receipt

import scala.concurrent.{ExecutionContext, Future}

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase {

  implicit val ac = appConfig
  val cdsFileUploadConnector = mock[CdsFileUploadConnector]
  val page = mock[upload_your_files_receipt]

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesReceiptController(
      new FakeAuthAction(),
      new FakeEORIAction(),
      getData,
      new FileUploadResponseRequiredAction(),
      cdsFileUploadConnector,
      page
    )(mcc, executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)

    super.afterEach()
  }

  def addFilenames(uploads: List[FileUpload]): List[FileUpload] = uploads.map(u => u.copy(filename = "someFile.pdf"))

  "onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll { (response: FileUploadResponse, cache: CacheMap) =>
          response.uploads.foreach { u =>
            when(cdsFileUploadConnector.getNotification(any())(any()))
              .thenReturn(Future.successful(Option(Notification(u.reference, "SUCCESS", "someFile.pdf"))))
          }

          val updatedCache = cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))
          val result = controller(fakeDataRetrievalAction(updatedCache)).onPageLoad()(fakeRequest)

          status(result) mustBe OK
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
