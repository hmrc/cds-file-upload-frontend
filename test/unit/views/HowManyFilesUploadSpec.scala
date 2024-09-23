/*
 * Copyright 2024 HM Revenue & Customs
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
import models.FileUploadCount
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.twirl.api.HtmlFormat.Appendable
import utils.FakeRequestCSRFSupport._
import views.behaviours.IntViewBehaviours
import views.html.how_many_files_upload

class HowManyFilesUploadSpec extends DomAssertions with IntViewBehaviours[FileUploadCount] with ScalaCheckPropertyChecks {

  val mrn = arbitraryMrn.arbitrary.sample.get

  val form = new FileUploadCountProvider()()
  val page = instanceOf[how_many_files_upload]

  def createView(form: Form[FileUploadCount] = form): Document =
    asDocument(page(form, mrn)(fakeRequest.withCSRFToken, messages))

  val messagePrefix = "howManyFilesUpload"

  "How Many Files Upload Page" must {

    "have the page's title prefixed with 'Error:'" when {
      "the page has errors" in {
        val view = createView(form.withGlobalError("error.summary.title"))
        view.head.getElementsByTag("title").first.text must startWith("Error: ")
      }
    }

    behave like normalPage(() => createView(), messagePrefix)

    def createAppendable(form: Form[FileUploadCount]): Appendable =
      page(form, mrn)(fakeRequest.withCSRFToken, messages)

    behave like intPage(createAppendable, (form, i) => form.bind(Map("value" -> i.toString)), "value", messagePrefix)

    "display the correct guidance" in {
      val page = createView()
      val expectedGuidanceKeys: List[String] = List(
        "paragraph1",
        "paragraph2.heading",
        "paragraph2",
        "paragraph3.heading",
        "paragraph3",
        "listItem1",
        "listItem2",
        "listItem3",
        "listItem4",
        "warning"
      )
      for (key <- expectedGuidanceKeys) assertContainsText(page, messages(s"$messagePrefix.$key"))
    }

    "display inset text" in {
      assertContainsMessage(createView(), s"$messagePrefix.insetText")
    }

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page(form, mrn)(AuthenticatedRequest(fakeRequest.withCSRFToken, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(createView(), routes.ContactDetailsController.onPageLoad.url)
    }
  }
}
