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

import play.api.test.Helpers._
import views.html._

class ErrorPageControllerSpec extends ControllerSpecBase {

  "Error Controller" must {

    "return the correct view for upload error" in {
      val result = new ErrorPageController().uploadError()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe upload_error()(fakeRequest, messages, appConfig).toString
    }

    "return the correct view for generic error" in {
      val result = new ErrorPageController().error()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe generic_error()(fakeRequest, messages, appConfig).toString
    }
  }
}
