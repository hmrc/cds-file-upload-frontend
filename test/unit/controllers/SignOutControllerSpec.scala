/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar.{mock, reset, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.signed_out

class SignOutControllerSpec extends ControllerSpecBase {

  val page = mock[signed_out]

  def view(): String = page()(fakeRequest, messages).toString

  val controller = new SignOutController(mcc, page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "SignOut controller" should {

    "return the expected signed_out page" when {
      "signOut is invoked" in {
        val result = controller.signOut(fakeRequest)
        status(result) mustBe OK
      }
    }

    "sign out the user" when {
      "signOut is invoked" in {
        val result = controller.signOut(fakeRequest)
        status(result) mustBe OK
        await(result).newSession mustBe defined
      }
    }
  }
}
