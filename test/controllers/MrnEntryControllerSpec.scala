/*
 * Copyright 2018 HM Revenue & Customs
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
import domain.auth.SignedInUser
import forms.MRNFormProvider
import generators.SignedInUserGen
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import play.api.test.Helpers.status
import views.html.mrn_entry
import play.api.test.Helpers._



class MrnEntryControllerSpec extends ControllerSpecBase with PropertyChecks with SignedInUserGen {

  val form = new MRNFormProvider()()

  def controller(signedInUser: SignedInUser) =
    new MrnEntryController(messagesApi, new FakeAuthAction(signedInUser), new FakeEORIAction, new MRNFormProvider, appConfig)

  def viewAsString(form: Form[_] = form) = mrn_entry(form)(fakeRequest, messages, appConfig).toString

  "Mrn Entry Page" must {
    "load the correct page when user is logged in " in {

      forAll { user: SignedInUser =>
        withSignedInUser(user) {
          val result = controller(user).onPageLoad(fakeRequest)

          status(result) mustBe OK
          contentAsString(result) mustBe viewAsString(form)
        }
      }
    }

    "mrn should be displayed if it exist on the cache" in {

    }
  }
}
