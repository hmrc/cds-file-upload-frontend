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

import generators.Generators
import models.requests.{AuthenticatedRequest, SignedInUser}
import models.{FileUpload, MRN, Successful}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import views.base.UnitViewSpec
import views.html.upload_your_files_confirmation

import scala.jdk.CollectionConverters._

class UploadYourFilesConfirmationSpec extends UnitViewSpec with Generators with ScalaCheckPropertyChecks {

  private val page = instanceOf[upload_your_files_confirmation]
  private val mrn = MRN("20GB46J8TMJ4RFGVA0").get
  private val email = "example@email.com"

  private val sampleFileUpload: FileUpload = FileUpload("reference", Successful, "filename", "id")

  private def view: Document = page(List(sampleFileUpload), Some(mrn), email)(request, messages)

  "File Upload Confirmation Page" should {

    "include the 'Sign out' link if the user is authorised" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn), email)(AuthenticatedRequest(request, user), messages)
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

    "display the language toggle" in {
      view.getElementsByClass("hmrc-language-select").text() must be("English Newid yr iaith ir Gymraeg Cymraeg")
    }

    "display the file receipts table" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn), email)(AuthenticatedRequest(request, user), messages)

        val tableRows = view.getElementsByTag("tbody").iterator().asScala.toList
        val rowsWithFiles = tableRows.zip(fileUploads)

        rowsWithFiles.foreach { case (row, file) =>
          row must containText(file.filename)
          row must containText(file.reference)
        }
      }
    }

    "have a link to print the page" in {
      assertContainsText(view, messages("fileUploadConfirmationPage.printPage"))
    }

    "have a paragraph explaining what happens when a query is raised" which {
      "has a heading" in {
        view.getElementsByTag("h2").get(1) must containMessage("fileUploadConfirmationPage.section1.title")
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
        view.getElementsByTag("h2").get(2) must containMessage("fileUploadConfirmationPage.section2.title")
      }

      "has some description text" in {
        view.getElementsByTag("p").get(3) must containMessage("fileUploadConfirmationPage.section2.paragraph1")
      }

      "have a bullet list" in {
        val bulletList = view.getElementsByTag("ul").get(1)

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
