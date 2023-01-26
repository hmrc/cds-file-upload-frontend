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

import models.SignOutReason
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.user_signed_out

class SignOutControllerSpec extends ControllerSpecBase with ScalaFutures {

  private val userSignedOutPage = mock[user_signed_out]

  private val controller = new SignOutController(stubMessagesControllerComponents(), userSignedOutPage)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(userSignedOutPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(userSignedOutPage)

    super.afterEach()
  }

  "SignOutController on signOut" when {

    "provided with SessionTimeout parameter" should {

      "return 303 (SEE_OTHER) status" in {
        val result = controller.signOut(SignOutReason.SessionTimeout)(fakeRequest)
        status(result) mustBe SEE_OTHER
      }

      "redirect to /we-signed-you-out" in {
        val result = controller.signOut(SignOutReason.SessionTimeout)(fakeRequest)
        redirectLocation(result) mustBe Some(controllers.routes.SignOutController.sessionTimeoutSignedOut.url)
      }
    }

    "provided with UserAction parameter" should {

      "return 303 (SEE_OTHER) status" in {
        val result = controller.signOut(SignOutReason.UserAction)(fakeRequest)
        status(result) mustBe SEE_OTHER
      }

      "redirect to /you-have-signed-out" in {
        val result = controller.signOut(SignOutReason.UserAction)(fakeRequest)
        redirectLocation(result) mustBe Some(controllers.routes.SignOutController.userSignedOut.url)
      }
    }
  }

  "SignOutController on sessionTimeoutSignedOut" should {

    val controller = new SignOutController(mcc, userSignedOutPage)

    "call sessionTimedOutPage" in {
      controller.sessionTimeoutSignedOut()(fakeRequest).futureValue
      verify(userSignedOutPage).apply(equalTo("session.timeout.heading"))(any(), any())
    }

    "return 200 status" in {
      val result = controller.sessionTimeoutSignedOut()(fakeRequest)
      status(result) mustBe OK
    }
  }

  "SignOutController on userSignedOut" should {

    val controller = new SignOutController(mcc, userSignedOutPage)

    "call userSignedOutPage" in {
      controller.userSignedOut()(fakeRequest).futureValue
      verify(userSignedOutPage).apply(equalTo("signed.out.heading"))(any(), any())
    }

    "return 200 status" in {
      val result = controller.userSignedOut()(fakeRequest)
      status(result) mustBe OK
    }
  }
}
