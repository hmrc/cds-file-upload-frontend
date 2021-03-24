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

import generators.Generators
import models.requests.{AuthenticatedRequest, SignedInUser}
import models.{FileUpload, MRN}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.upload_your_files_receipt
import views.matchers.ViewMatchers

class UploadYourFilesReceiptSpec extends DomAssertions with ViewBehaviours with ViewMatchers with ScalaCheckPropertyChecks with Generators {

  val page = instanceOf[upload_your_files_receipt]
  val mrn = MRN("20GB46J8TMJ4RFGVA0").get
  val printHref = "javascript:if(window.print)window.print()"

  def view(receipts: List[FileUpload]): Html = page(receipts, Some(mrn))(fakeRequest, messages)

  val view: () => Html = () => view(Nil)

  val pagePrefix = "fileUploadReceiptPage"

  "File Upload Receipt Page" must {
    behave like normalPage(
      view,
      pagePrefix,
      "whatHappensNext",
      "paragraph1",
      "paragraph2",
      "paragraph3",
      "listitem1",
      "listitem2",
      "listitem3",
      "listitem4",
      "listitem5"
    )

    "include the 'Sign out' link if the user is authorised" in {
      forAll { (fileUploads: List[FileUpload], user: SignedInUser) =>
        val view = page(fileUploads, Some(mrn))(AuthenticatedRequest(fakeRequest, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "have title" in {

      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        assertEqualsMessage(doc, "title", s"$pagePrefix.heading")
      }
    }

    "have heading" in {

      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        assertH1EqualsMessage(doc, s"$pagePrefix.heading", fileUploads.length)
      }
    }

    "display the mrn value" in {

      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        assertContainsValue(doc, ".govuk-panel__body > strong:nth-child(2)", mrn.value)
      }
    }

    "display all receipts" in {

      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        fileUploads.foreach { fu =>
          assertContainsText(doc, fu.reference)
        }
      }
    }

    "have a links to print the page" in {
      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        assertContainsLink(doc, messages(s"${pagePrefix}.printPage"), printHref)
      }
    }

    "have a paragraph expaining next steps" in {
      val paragraph = asDocument(view()).getElementsByTag("p").get(2)

      paragraph must containMessage(s"${pagePrefix}.paragraph1")
    }

    "have a bullet list" in {
      val bulletList = asDocument(view()).getElementsByTag("ul").get(0)

      bulletList must containMessage(s"${pagePrefix}.listitem1")
      bulletList must containMessage(s"${pagePrefix}.listitem2")
      bulletList must containMessage(s"${pagePrefix}.listitem3")
    }

    "have a second bullet list" in {
      val bulletList = asDocument(view()).getElementsByTag("ul").get(1)

      bulletList must containMessage(s"${pagePrefix}.listitem4")
      bulletList must containMessage(s"${pagePrefix}.listitem5")
    }

    "have a links to restart the journey" in {
      forAll { fileUploads: List[FileUpload] =>
        val doc = asDocument(view(fileUploads))

        assertContainsLink(doc, messages(s"${pagePrefix}.finalButton.text"), controllers.routes.RootController.displayPage().url)
      }
    }
  }
}
