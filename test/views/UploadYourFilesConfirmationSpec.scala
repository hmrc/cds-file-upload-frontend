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

import base.FilesUploadedSpec
import models.{FileUpload, MRN}
import models.requests.{AuthenticatedRequest, SignedInUser}
import scala.collection.JavaConverters._
import org.jsoup.nodes.Document
import views.html.upload_your_files_confirmation
import views.matchers.ViewMatchers

class UploadYourFilesConfirmationSpec extends DomAssertions with ViewMatchers with FilesUploadedSpec {

  val page = instanceOf[upload_your_files_confirmation]
  val mrn = MRN("20GB46J8TMJ4RFGVA0").get
  val email = "example@email.com"
  val pagePrefix = "fileUploadConfirmationPage"

  val view: Document = page(List(sampleFileUpload), Some(mrn), email)(fakeRequest, messages)

  "File Upload Confirmation Page" should {

    "include the 'Sign out' link if the user is authorised" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn), email)(AuthenticatedRequest(fakeRequest, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display page header" which {
      "contains the header text" in {
        view.getElementsByTag("h1").first() must containMessage(s"$pagePrefix.heading")
      }

      "display the mrn value" in {
        view.getElementsByClass("govuk-panel__body").first() must containText(mrn.value)
      }
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

    "have a links to print the page" in {
      assertContainsLink(view, messages(s"$pagePrefix.printPage"), "javascript:if(window.print)window.print()")
    }

    "have a paragraph explaining next steps" which {
      "has a heading" in {
        view.getElementsByTag("h3").first() must containMessage(s"$pagePrefix.section1.title")
      }

      "has some description text" in {
        view.getElementsByTag("p").get(2) must containMessage(s"$pagePrefix.section1.paragraph1")
      }

      "have a bullet list" in {
        val bulletList = view.getElementsByTag("ul").get(0)

        bulletList must containMessage(s"${pagePrefix}.section1.listitem1")
        bulletList must containMessage(s"${pagePrefix}.section1.listitem2")
        bulletList must containMessage(s"${pagePrefix}.section1.listitem3")
      }

      "has a link to the NCH" in {
        assertContainsLink(
          view,
          messages(s"$pagePrefix.section1.paragraph2.linkText"),
          "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/national-clearance-hub"
        )
      }
    }

    "have a paragraph explaining what happens when a query is raised" which {
      "has a heading" in {
        view.getElementsByTag("h3").get(1) must containMessage(s"$pagePrefix.section2.title")
      }

      "displays the current user's verified email address" in {
        view.getElementById("verifiedEmail") must containText(email)
      }

      "has a link to the secure messages filter page" in {
        assertContainsLink(view, messages(s"$pagePrefix.section2.paragraph2.linkText"), controllers.routes.InboxChoiceController.onPageLoad().url)
      }
    }

    "have a links to restart the journey" in {
      assertContainsLink(view, messages(s"${pagePrefix}.finalButton.text"), controllers.routes.ChoiceController.onPageLoad().url)
    }
  }
}
