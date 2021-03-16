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

package views

import controllers.routes
import forms.FileUploadCountProvider
import models.requests.{AuthenticatedRequest, SignedInUser}
import models.{FileUploadCount, MRN}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.behaviours.IntViewBehaviours
import views.html.how_many_files_upload

class HowManyFilesUploadSpec extends DomAssertions with IntViewBehaviours[FileUploadCount] with ScalaCheckPropertyChecks {

  val form = new FileUploadCountProvider()()
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  val page = instanceOf[how_many_files_upload]
  val view = page(form, mrn)(fakeRequest.withCSRFToken, messages)

  val messagePrefix = "howManyFilesUpload"

  def createViewUsingForm: Form[FileUploadCount] => HtmlFormat.Appendable =
    form => page(form, mrn)(fakeRequest.withCSRFToken, messages)

  "How Many Files Upload Page" must {
    behave like normalPage(() => view, messagePrefix)

    behave like intPage(createViewUsingForm, (form, i) => form.bind(Map("value" -> i.toString)), "value", messagePrefix)

    "display the correct guidance" in {
      val expectedGuidanceKeys: List[String] = List("paragraph1", "paragraph2", "paragraph3", "listItem1", "listItem2", "listItem3", "listItem4")
      for (key <- expectedGuidanceKeys) assertContainsText(asDocument(view), messages(s"$messagePrefix.$key"))
    }

    "display inset text" in {
      assertContainsMessage(asDocument(view), s"$messagePrefix.insetText")
    }

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page(form, mrn)(AuthenticatedRequest(fakeRequest.withCSRFToken, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(asDocument(view), routes.ContactDetailsController.onPageLoad.url)
    }
  }
}
