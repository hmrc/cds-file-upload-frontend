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

import controllers.actions.{DataRetrievalAction, FakeAuthAction, FakeEORIAction}
import forms.FileUploadCountProvider
import generators.Generators
import models.{FileUploadCount, MRN}
import models.requests.SignedInUser
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.HowManyFilesUploadPage
import play.api.data.Form
import play.api.libs.json.JsNumber
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.how_many_files_upload

class HowManyFilesUploadControllerSpec extends ControllerSpecBase
  with MockitoSugar
  with PropertyChecks
  with Generators
  with BeforeAndAfterEach{

  val form = new FileUploadCountProvider()()

  def controller(signedInUser: SignedInUser, dataRetrieval: DataRetrievalAction = getEmptyCacheMap) =
    new HowManyFilesUploadController(
      messagesApi,
      new FakeAuthAction(signedInUser),
      new FakeEORIAction,
      dataRetrieval,
      new FileUploadCountProvider,
      dataCacheConnector,
      appConfig)

  def viewAsString(form: Form[_] = form) = how_many_files_upload(form)(fakeRequest, messages, appConfig).toString

  "How Many Files Upload Page" must {
    "load correct page when user is logged in " in {
      forAll { user: SignedInUser =>

        val result = controller(user).onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(form)
      }
    }

    "File count should be displayed if it exist on the cache" in {

      forAll { (user: SignedInUser, fileUploadCount: FileUploadCount) =>

        val cacheMap: CacheMap = CacheMap("", Map(HowManyFilesUploadPage.toString -> JsNumber(fileUploadCount.value)))
        val result = controller(user, getCacheMap(cacheMap)).onPageLoad(fakeRequest)

        contentAsString(result) mustBe viewAsString(form.fill(fileUploadCount))
      }
    }
  }
}
