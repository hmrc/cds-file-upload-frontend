/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.CdsFileUploadConnector
import models.{FileUploadResponse, Notification, UserAnswers}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testdata.CommonTestData.eori
import views.html.upload_your_files_receipt

import scala.concurrent.Future

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase with SfusMetricsMock {

  private val cdsFileUploadConnector = mock[CdsFileUploadConnector]
  private val page = mock[upload_your_files_receipt]

  private val controller = new UploadYourFilesReceiptController(
    new FakeAuthAction(),
    new FakeVerifiedEmailAction(),
    cdsFileUploadConnector,
    sfusMetrics,
    page,
    mockAnswersConnector
  )(mcc, executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockAnswersConnector.findByEori(anyString())).thenReturn(Future.successful(None))
    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page, mockAnswersConnector)

    super.afterEach()
  }

  "onPageLoad" should {

    "load the view" when {

      "request file exists in response" in {

        forAll { response: FileUploadResponse =>
          response.uploads.foreach { u =>
            when(cdsFileUploadConnector.getNotification(any())(any()))
              .thenReturn(Future.successful(Option(Notification(u.reference, "SUCCESS", "someFile.pdf"))))
          }

          val answers = UserAnswers(eori, fileUploadResponse = Some(response))
          when(mockAnswersConnector.findByEori(anyString())).thenReturn(Future.successful(Some(answers)))

          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe OK
          result.map(_ => verify(mockAnswersConnector).removeByEori(any()))
        }
      }
    }

    "redirect to start page" when {

      "no responses are in the cache" in {

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.StartController.displayStartPage().url)

        result.map(_ => verify(mockAnswersConnector).removeByEori(any()))
      }
    }
  }
}
