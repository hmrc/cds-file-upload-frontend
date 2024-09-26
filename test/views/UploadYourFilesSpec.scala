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

import base.UnitViewSpec
import controllers.routes.Assets
import generators.Generators
import models._
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import views.html.upload_your_files

class UploadYourFilesSpec extends UnitViewSpec with Generators with ScalaCheckPropertyChecks {

  private val page = instanceOf[upload_your_files]
  private val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  private val uploadRequest = UploadRequest("", Map.empty)

  def view(pos: Position = First(3)): Document = asDocument(page(uploadRequest, pos, mrn)(request, messages))

  val messagePrefix = "fileUploadPage"

  "Upload your files page" must {

    behave like pageWithoutHeading(() => view(), messagePrefix, "paragraph1", "listItem1", "listItem2", "listItem3", "listItem4")

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val authenticatedRequest = AuthenticatedRequest(request, user)
        val view = page(uploadRequest, First(3), mrn)(authenticatedRequest, messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "not display the 'Back' link" in {
      assertBackLinkIsNotIncluded(view())
    }

    "display the 'Cancel upload' link" in {
      assertContainsLink(view(), "Cancel upload", controllers.routes.HowManyFilesUploadController.onPageLoad.url)
    }

    "display inset text" in {
      assertContainsMessage(view(), s"$messagePrefix.insetText")
    }

    "show title" when {
      val service = messages("service.name")

      "first file upload is shown" in {
        forAll { total: Int =>
          val message = messages(s"$messagePrefix.heading.first")
          assertEqualsMessage(view(First(total)), "title", "title.format", message, service)
        }
      }

      "a middle file upload is shown" in {
        forAll { (index: Int, total: Int) =>
          val message = messages(s"$messagePrefix.heading.middle")
          assertEqualsMessage(view(Middle(index, total)), "title", "title.format", message, service)
        }
      }

      "the last file upload is shown" in {
        forAll { total: Int =>
          val message = messages(s"$messagePrefix.heading.last")
          assertEqualsMessage(view(Last(total)), "title", "title.format", message, service)
        }
      }
    }

    "show heading" when {

      "first file upload is shown" in {
        forAll { total: Int =>
          assertH1EqualsMessage(view(First(total)), s"$messagePrefix.heading.first")
        }
      }

      "a middle file upload is shown" in {
        forAll { (index: Int, total: Int) =>
          assertH1EqualsMessage(view(Middle(index, total)), s"$messagePrefix.heading.middle")
        }
      }

      "the last file upload is shown" in {
        forAll { total: Int =>
          assertH1EqualsMessage(view(Last(total)), s"$messagePrefix.heading.last")
        }
      }
    }

    "show file upload counter" when {

      "a file middle is requested" in {
        forAll { (index: Int, total: Int) =>
          val doc = view(Middle(index, total))
          assertContainsMessage(doc, s"$messagePrefix.filesUploaded", index - 1, total)
        }
      }

      "the last file is requested" in {
        forAll { total: Int =>
          val doc = view(Last(total))
          assertContainsMessage(doc, s"$messagePrefix.filesUploaded", total - 1, total)
        }
      }
    }

    "contain the Javascript file for validating files to upload" in {
      val script = view().getElementById("validation")
      script.attr("src") mustBe Assets.versioned("javascripts/validation.js").url
    }

    "contain the Javascript file defining validation error messages" in {
      val script = view().getElementById("validation-messages")
      Option(script).isDefined mustBe true
    }
  }
}
