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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import models.requests.SignedInUser
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.file_warning

class FileWarningControllerSpec extends ControllerSpecBase {

  val fileWarning = mock[file_warning]

  def controller(signedInUser: SignedInUser, eori: String) =
    new FileWarningController(new FakeAuthAction(signedInUser), new FakeEORIAction(eori), mcc, fileWarning)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(fileWarning.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(fileWarning)

    super.afterEach()
  }

  "File Warning Page" must {

    "load the correct page when user is logged in" in {
      forAll { (user: SignedInUser, eori: String) =>
        val result = controller(user, eori).onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }
  }
}
