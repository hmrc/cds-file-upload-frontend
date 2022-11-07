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

import generators.Generators
import models._
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import views.behaviours.ViewBehaviours
import views.html.upload_your_files

import scala.jdk.CollectionConverters.ListHasAsScala

class UploadYourFilesSpec extends DomAssertions with ViewBehaviours with ScalaCheckPropertyChecks with Generators {

  val page = instanceOf[upload_your_files]
  val mrn: MRN = arbitraryMrn.arbitrary.sample.get
  val uploadRequest = UploadRequest("", Map.empty)

  def view(pos: Position = First(3)): Document = asDocument(page(uploadRequest, pos, mrn)(fakeRequest, messages))

  val messagePrefix = "fileUploadPage"

  "Upload your files page" must {

    behave like pageWithoutHeading(() => view(), messagePrefix, "paragraph1", "listItem1", "listItem2", "listItem3", "listItem4")

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val request = AuthenticatedRequest(fakeRequest, user)
        val view = page(uploadRequest, First(3), mrn)(request, messages)
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
      val service = messages("common.service.name")

      "first file upload is shown" in {
        forAll { total: Int =>
          val message = messages(s"$messagePrefix.heading.first")
          assertEqualsMessage(view(First(total)), "title", "common.title.format", message, service)
        }
      }

      "a middle file upload is shown" in {
        forAll { (index: Int, total: Int) =>
          val message = messages(s"$messagePrefix.heading.middle")
          assertEqualsMessage(view(Middle(index, total)), "title", "common.title.format", message, service)
        }
      }

      "the last file upload is shown" in {
        forAll { total: Int =>
          val message = messages(s"$messagePrefix.heading.last")
          assertEqualsMessage(view(Last(total)), "title", "common.title.format", message, service)
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
      val scripts = view().getElementsByTag("script").asScala
      scripts.last.attr("src") mustBe "/cds-file-upload-service/assets/javascripts/validation.min.js"
    }
  }
}
