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

package views

import generators.Generators
import models.FileUpload
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours

class UploadYourFilesReceiptSpec extends DomAssertions with ViewBehaviours with PropertyChecks with Generators {

  def view(receipts: List[FileUpload]): Html = views.html.upload_your_files_receipt(receipts)(fakeRequest, messages, appConfig)

  val view: () => Html = () => view(Nil)

  val pagePrefix = "fileUploadReceiptPage"

  "File Upload Receipt Page" must {
    behave like pageWithoutHeading(view, pagePrefix, "whatHappensNext", "paragraph1", "paragraph2", "listitem1", "listitem2", "listitem3")

    "have title" in {

      forAll { fileUploads: List[FileUpload] =>

        val doc = asDocument(view(fileUploads))

        assertEqualsMessage(doc, "title", s"$pagePrefix.title")
      }
    }

    "have heading" in {

      forAll { fileUploads: List[FileUpload] =>

        val doc = asDocument(view(fileUploads))

        assertH1EqualsMessage(doc, s"$pagePrefix.heading", fileUploads.length)
      }
    }

    "display all receipts" in {

      forAll { fileUploads: List[FileUpload] =>

        val doc = asDocument(view(fileUploads))

        fileUploads.foreach { fu =>
          assertContainsText(doc, fu.filename)
          assertContainsText(doc, fu.reference)
        }
      }
    }
  }
}
