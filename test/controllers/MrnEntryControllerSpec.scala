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

import controllers.actions.{DataRetrievalAction, FakeAuthAction, FakeEORIAction}
import domain.MRN
import domain.auth.SignedInUser
import forms.MRNFormProvider
import generators.Generators
import org.scalatest.prop.PropertyChecks
import pages.MrnEntryPage
import play.api.data.Form
import play.api.libs.json.{JsString, JsValue}
import play.api.test.Helpers.status
import views.html.mrn_entry
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap


class MrnEntryControllerSpec extends ControllerSpecBase with PropertyChecks with Generators {

  val form = new MRNFormProvider()()

  def controller(signedInUser: SignedInUser, dataRetrieval: DataRetrievalAction = getEmptyCacheMap) =
    new MrnEntryController(
      messagesApi,
      new FakeAuthAction(signedInUser),
      new FakeEORIAction,
      dataRetrieval,
      new MRNFormProvider,
      appConfig)

  def viewAsString(form: Form[_] = form) = mrn_entry(form)(fakeRequest, messages, appConfig).toString

  "Mrn Entry Page" must {
    "load the correct page when user is logged in " in {

      forAll { user: SignedInUser =>

        val result = controller(user).onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(form)
      }
    }

    "mrn should be displayed if it exist on the cache" in {

      forAll { (user: SignedInUser, mrn: MRN) =>

        val cacheMap: CacheMap = CacheMap("", Map(MrnEntryPage.toString -> JsString(mrn.value)))
        val result = controller(user, getCacheMap(cacheMap)).onPageLoad(fakeRequest)

        contentAsString(result) mustBe viewAsString(form.fill(mrn))
      }
    }
  }
}
