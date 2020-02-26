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

import controllers.actions.ContactDetailsRequiredAction
import forms.MRNFormProvider
import models.requests.SignedInUser
import models.{ContactDetails, MRN}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import pages.MrnEntryPage
import play.api.libs.json.JsString
import play.api.test.Helpers.{status, _}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.mrn_entry

class MrnEntryControllerSpec extends ControllerSpecBase {

  val form = new MRNFormProvider()()

  val contactDetailsRequiredGen =
    for {
      cache <- arbitrary[CacheMap]
      contactDetails <- arbitrary[ContactDetails]
    } yield {
      fakeContactDetailsRequiredAction(cache, contactDetails)
    }

  val page = mock[mrn_entry]

  def controller(signedInUser: SignedInUser, eori: String, requireContactDetails: ContactDetailsRequiredAction) =
    new MrnEntryController(
      new FakeAuthAction(signedInUser),
      new FakeEORIAction(eori),
      requireContactDetails,
      new FakeDataRetrievalAction(None),
      new MRNFormProvider,
      mockDataCacheConnector,
      mcc,
      page
    )(executionContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)

    super.afterEach()
  }

  "Mrn Entry Page" must {
    "load the correct page when user is logged in " in {

      forAll(arbitrary[SignedInUser], arbitrary[String], contactDetailsRequiredGen) { (user, eori, fakeContactDetails) =>
        val result = controller(user, eori, fakeContactDetails).onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }

    "mrn should be displayed if it exist on the cache" in {

      forAll(arbitrary[SignedInUser], arbitrary[String], arbitrary[MRN]) { (user, eori, mrn) =>
        val fakeContactDetails =
          new FakeContactDetailsRequiredAction(CacheMap("", Map(MrnEntryPage.toString -> JsString(mrn.value))), ContactDetails("", "", "", ""))
        val result = controller(user, eori, fakeContactDetails).onPageLoad(fakeRequest)

        status(result) mustBe OK
      }
    }

    "return an ok when valid data is submitted" in {

      forAll(arbitrary[SignedInUser], arbitrary[String], arbitrary[MRN], contactDetailsRequiredGen) { (user, eori, mrn, fakeContactDetails) =>
        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn.value)
        val result = controller(user, eori, fakeContactDetails).onSubmit(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileWarningController.onPageLoad().url)
      }
    }

    "return a bad request when invalid data is submitted" in {

      forAll(arbitrary[SignedInUser], arbitrary[String], arbitrary[String], contactDetailsRequiredGen) { (user, eori, mrn, fakeContactDetails) =>
        whenever(!mrn.matches(MRN.validRegex)) {

          val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn)

          val result = controller(user, eori, fakeContactDetails).onSubmit(postRequest)

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "save data in cache when valid" in {

      forAll(arbitrary[SignedInUser], arbitrary[String], arbitrary[MRN]) { (user, eori, mrn) =>
        val expectedMap = CacheMap(user.internalId, Map(MrnEntryPage.toString -> JsString(mrn.value)))
        val fakeContactDetails = new FakeContactDetailsRequiredAction(expectedMap, ContactDetails("", "", "", ""))
        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> mrn.value)
        await(controller(user, eori, fakeContactDetails).onSubmit(postRequest))

        verify(mockDataCacheConnector).save(eqTo(expectedMap))(any[HeaderCarrier])
      }
    }
  }
}
