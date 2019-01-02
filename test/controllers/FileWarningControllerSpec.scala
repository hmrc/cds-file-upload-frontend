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

import controllers.actions.{FakeAuthAction, FakeEORIAction}
import generators.Generators
import models.requests.SignedInUser
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import views.html.file_warning
import play.api.test.Helpers._

class FileWarningControllerSpec extends ControllerSpecBase with MockitoSugar with PropertyChecks with Generators {

  def controller(signedInUser: SignedInUser, eori: String) =
    new FileWarningController(messagesApi, new FakeAuthAction(signedInUser), new FakeEORIAction(eori), appConfig)

  def viewAsString() = file_warning()(fakeRequest, messages, appConfig).toString

  "File Warning Page" must {

    "load the correct page when user is logged in" in {
      forAll { (user: SignedInUser, eori: String) =>
        val result = controller(user, eori).onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }
    }
  }
}
