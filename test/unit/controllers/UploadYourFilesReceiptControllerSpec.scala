/*
 * Copyright 2023 HM Revenue & Customs
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

import base.{FilesUploadedSpec, SfusMetricsMock}
import connectors.CdsFileUploadConnector
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testdata.CommonTestData.eori
import views.html.upload_your_files_confirmation

import scala.concurrent.Future

class UploadYourFilesReceiptControllerSpec extends ControllerSpecBase with SfusMetricsMock with FilesUploadedSpec {

  private val cdsFileUploadConnector = mock[CdsFileUploadConnector]
  private val confirmationPage = mock[upload_your_files_confirmation]

  private val controller = new UploadYourFilesReceiptController(
    new FakeAuthAction(),
    new FakeVerifiedEmailAction(),
    cdsFileUploadConnector,
    sfusMetrics,
    confirmationPage,
    mockFileUploadAnswersService
  )(mcc, executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(cdsFileUploadConnector, confirmationPage)
  }

  "UploadYourFilesReceiptController onPageLoad" should {

    "load the confirmation view" in {
      withSingleFileSuccessfullyUploaded {

        when(confirmationPage.apply(any(), any(), any())(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
        verify(confirmationPage).apply(any(), any(), any())(any[Request[_]], any[Messages])
      }
    }

    "redirect to start page" when {
      "no user answers are in the cache" in {
        when(mockFileUploadAnswersService.findOne(anyString()))
          .thenReturn(Future.successful(None))

        val result = controller.onPageLoad()(fakeRequest).futureValue

        result.header.status mustBe SEE_OTHER
        result.header.headers.get(LOCATION) mustBe Some(routes.RootController.displayPage.url)
      }
    }

    "clear the answers cache" in {
      withSingleFileSuccessfullyUploaded {
        when(confirmationPage.apply(any(), any(), any())(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result = controller.onPageLoad()(fakeRequest).futureValue

        result.header.status mustBe OK

        verify(mockFileUploadAnswersService).findOne(any())
      }
    }

    "user is redirect to start page" in {
      when(mockFileUploadAnswersService.findOne(anyString()))
        .thenReturn(Future.successful(None))

      val result = controller.onPageLoad()(fakeRequest).futureValue

      result.header.status mustBe SEE_OTHER
      verify(mockFileUploadAnswersService).findOne(any())
    }
  }

  def withSingleFileSuccessfullyUploaded(test: => Unit): Unit = {
    when(cdsFileUploadConnector.getNotification(any())(any()))
      .thenReturn(Future.successful(Option(Notification(sampleFileUpload.reference, "SUCCESS", Some("someFile.pdf")))))

    val answers = FileUploadAnswers(eori, fileUploadResponse = Some(sampleFileUploadResponse))
    when(mockFileUploadAnswersService.findOne(anyString()))
      .thenReturn(Future.successful(Some(answers)))

    test
  }
}
