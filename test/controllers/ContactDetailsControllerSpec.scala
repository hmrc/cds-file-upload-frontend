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

import controllers.actions.{DataRetrievalAction, FakeActions}
import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models._
import models.requests.SignedInUser
import org.mockito.Mockito.{times, verify}
import org.scalacheck.Arbitrary._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.ContactDetailsPage
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.contact_details

class ContactDetailsControllerSpec extends ControllerSpecBase
  with ScalaFutures
  with MockitoSugar
  with PropertyChecks
  with Generators
  with FakeActions {

  val form = Form(contactDetailsMapping)

  def view(form: Form[_] = form): String = contact_details(form)(fakeRequest, messages, appConfig).toString

  def controller(signedInUser: SignedInUser, eori: String, dataRetrieval: DataRetrievalAction = getEmptyCacheMap) =
    new ContactDetailsController(
      messagesApi,
      new FakeAuthAction(signedInUser),
      new FakeEORIAction(eori),
      dataRetrieval,
      dataCacheConnector,
      appConfig
    )

  "Contact details page" should {

    "load the correct page when user is logged in" in {

      forAll { (user: SignedInUser, eori: String) =>

        val result = controller(user, eori).onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe view(form)
      }
    }

    "contact details should be displayed if it exist on the cache" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>

        val cacheMap: CacheMap = CacheMap("", Map(ContactDetailsPage.toString -> Json.toJson(contactDetails)))
        val result = controller(user, eori, getCacheMap(cacheMap)).onPageLoad(fakeRequest)

        contentAsString(result) mustBe view(form.fill(contactDetails))
      }
    }

    "return an ok when valid data is submitted" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>

        val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(contactDetails): _*)
        val result = controller(user, eori).onSubmit(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.MrnEntryController.onPageLoad().url)
      }
    }

    "return a bad request when invalid data is submitted" in {

      forAll(arbitrary[SignedInUser], arbitrary[String], arbitrary[ContactDetails], minStringLength(36)) {
        (user, eori, contactDetails, invalidName) =>

          val badData = contactDetails.copy(name = invalidName)

          val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(badData): _*)
          val badForm = form.fillAndValidate(badData)

          val result = controller(user, eori).onSubmit(postRequest)

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(badForm)
      }
    }

    "save data in cache when valid" in {

      forAll { (user: SignedInUser, eori: String, contactDetails: ContactDetails) =>

        val postRequest = fakeRequest.withFormUrlEncodedBody(asFormParams(contactDetails): _*)
        await(controller(user, eori).onSubmit(postRequest))

        val expectedMap = CacheMap(user.internalId, Map(ContactDetailsPage.toString -> Json.toJson(contactDetails)))
        verify(dataCacheConnector, times(1)).save(expectedMap)
      }
    }
  }
}
