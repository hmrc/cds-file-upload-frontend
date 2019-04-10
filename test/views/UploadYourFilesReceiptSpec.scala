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
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.upload_your_files_receipt

class UploadYourFilesReceiptSpec extends DomAssertions with ViewBehaviours with PropertyChecks with Generators {

  def view(receipts: List[String]): Html =
    upload_your_files_receipt(receipts)(fakeRequest, messages, appConfig)

  val view: () => Html = () => view(Nil)

  val messagePrefix = "fileUploadReceiptPage"

  val messageKeys = List(
    "whatHappensNext",
    "paragraph1",
    "paragraph2",
    "helpline",
    "imports",
    "imports.tel",
    "exports",
    "exports.tel"
  )

  "File Upload Receipt Page" must {
    behave like pageWithoutHeading(view, messagePrefix, messageKeys: _*)

    "have title" in {

      forAll { receipts: List[String] =>

        val doc = asDocument(view(receipts))

        assertEqualsMessage(doc, "title", s"$messagePrefix.title")
      }
    }

    "have heading" in {

      forAll { receipts: List[String] =>

        val doc = asDocument(view(receipts))

        assertH1EqualsMessage(doc, s"$messagePrefix.heading", receipts.length)
      }
    }

    "display all receipts" in {

      forAll(Gen.listOf(saneString)) { receipts: List[String] =>

        val doc = asDocument(view(receipts))

        receipts.foreach(assertContainsText(doc, _))
      }
    }
  }
}
