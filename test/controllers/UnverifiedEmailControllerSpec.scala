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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.unverified_email

class UnverifiedEmailControllerSpec extends ControllerSpecBase {

  val page = mock[unverified_email]

  def view(): String = page("")(fakeRequest, messages).toString

  def controller() =
    new UnverifiedEmailController(new FakeAuthAction(), new FakeEORIAction(), mcc, page, appConfig)

  override protected def beforeEach(): Unit = {
    super.beforeEach

    reset(page)
    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  "UnverifiedEmailController" should {

    "display the unverified email detection page" in {

      val result = controller().informUser(fakeRequest)

      status(result) mustBe OK
    }
  }
}
