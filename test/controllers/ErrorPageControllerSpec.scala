/*
 * Copyright 2024 HM Revenue & Customs
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

import handlers.ErrorHandler
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, verify, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html._

import scala.concurrent.Future

class ErrorPageControllerSpec extends ControllerSpecBase {

  private val errorHandler = mock[ErrorHandler]
  private val uploadErrorPage = mock[upload_error]

  val controller = new ErrorPageController(mcc, uploadErrorPage, errorHandler)

  "Error Controller" must {

    "return the correct view for upload error" in {
      when(uploadErrorPage()(any(), any())).thenReturn(HtmlFormat.empty)
      val result = controller.uploadError()(fakeRequest)

      status(result) mustBe OK
      verify(uploadErrorPage).apply()(any(), any())
    }

    "call ErrorHandler.internalServerError for generic error" in {
      when(errorHandler.internalServerErrorTemplate(any())).thenReturn(Future.successful(HtmlFormat.empty))
      val result = controller.error()(fakeRequest)

      status(result) mustBe OK
      verify(errorHandler).internalServerErrorTemplate(any())
    }
  }
}
