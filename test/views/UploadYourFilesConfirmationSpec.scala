/*
 * Copyright 2022 HM Revenue & Customs
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

import base.FilesUploadedSpec
import models.requests.{AuthenticatedRequest, SignedInUser}
import models.{FileUpload, MRN}
import org.jsoup.nodes.Document
import views.html.upload_your_files_confirmation
import views.matchers.ViewMatchers

import scala.collection.JavaConverters._

class UploadYourFilesConfirmationSpec extends DomAssertions with ViewMatchers with FilesUploadedSpec {

  private val page = instanceOf[upload_your_files_confirmation]
  private val mrn = MRN("20GB46J8TMJ4RFGVA0").get
  private val email = "example@email.com"

  private def view: Document = page(List(sampleFileUpload), Some(mrn), email)(fakeRequest, messages)

  "File Upload Confirmation Page" should {

    "include the 'Sign out' link if the user is authorised" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn), email)(AuthenticatedRequest(fakeRequest, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display page header" which {
      "contains the header text" in {
        view.getElementsByTag("h1").first() must containMessage("fileUploadConfirmationPage.heading")
      }

      "display the mrn value" in {
        view.getElementsByClass("govuk-panel__body").first() must containText(mrn.value)
      }
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display the file receipts table" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn), email)(AuthenticatedRequest(fakeRequest, user), messages)

        val tableRows = view.getElementsByTag("tbody").iterator().asScala.toList
        val rowsWithFiles = tableRows.zip(fileUploads)

        rowsWithFiles.foreach {
          case (row, file) =>
            row must containText(file.filename)
            row must containText(file.reference)
        }
      }
    }

    "have a link to print the page" in {
      assertContainsLink(view, messages("fileUploadConfirmationPage.printPage"), "javascript:if(window.print)window.print()")
    }

    "have a paragraph explaining what happens when a query is raised" which {
      "has a heading" in {
        view.getElementsByTag("h2").first() must containMessage("fileUploadConfirmationPage.section1.title")
      }

      "displays the current user's verified email address" in {
        view.getElementById("verifiedEmail") must containText(email)
      }

      "has links to the secure messages filter page" in {
        assertContainsLink(
          view,
          messages("fileUploadConfirmationPage.section1.paragraph1.linkText"),
          controllers.routes.InboxChoiceController.onPageLoad.url
        )
        assertContainsLink(
          view,
          messages("fileUploadConfirmationPage.section1.paragraph2.linkText"),
          controllers.routes.InboxChoiceController.onPageLoad.url
        )
      }
    }

    "have a paragraph explaining next steps" which {
      "has a heading" in {
        view.getElementsByTag("h2").get(1) must containMessage("fileUploadConfirmationPage.section2.title")
      }

      "has some description text" in {
        view.getElementsByTag("p").get(4) must containMessage("fileUploadConfirmationPage.section2.paragraph1")
      }

      "have a bullet list" in {
        val bulletList = view.getElementsByTag("ul").get(0)

        bulletList must containMessage("fileUploadConfirmationPage.section2.listitem1")
        bulletList must containMessage("fileUploadConfirmationPage.section2.listitem2")
        bulletList must containMessage("fileUploadConfirmationPage.section2.listitem3")
      }

      "has a link to the NCH" in {
        containMessage("fileUploadConfirmationPage.section2.paragraph2")

        assertContainsLink(view, "nch.cds@hmrc.gov.uk", "mailto:nch.cds@hmrc.gov.uk")
      }
    }

    "have a links to restart the journey" in {
      assertContainsLink(view, messages("fileUploadConfirmationPage.finalButton.text"), controllers.routes.ChoiceController.onPageLoad.url)
    }
  }
}
