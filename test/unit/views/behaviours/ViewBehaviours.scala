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

package views.behaviours

import org.jsoup.nodes.Document
import views.DomAssertions

trait ViewBehaviours extends DomAssertions {

  def normalPage(view: () => Document, messageKeyPrefix: String, expectedGuidanceKeys: String*): Unit = {

    "behave like a page with a heading" when {
      "rendered" must {
        "display the correct page title" in {
          assertH1EqualsMessage(view(), s"$messageKeyPrefix.heading")
        }
      }
    }

    "behave like a page with a title" when {
      "rendered" must {
        "display the correct browser title" in {
          val message = messages(s"$messageKeyPrefix.heading")
          val service = messages("service.name")
          assertEqualsMessage(view(), "title", "title.format", message, service)
        }
      }
    }

    behave like pageWithoutHeading(view, messageKeyPrefix, expectedGuidanceKeys: _*)
  }

  def pageWithoutHeading(view: () => Document, messageKeyPrefix: String, expectedGuidanceKeys: String*): Unit =
    "behave like a normal page" when {
      "rendered" must {
        "have the correct banner title" in {
          val element = view().getElementsByClass("hmrc-header__service-name")
          element.text mustBe messages("service.name")
        }

        "display the correct guidance" in {
          val document = view()
          for (key <- expectedGuidanceKeys) assertContainsText(document, messages(s"$messageKeyPrefix.$key"))
        }
      }
    }

  def pageWithBackLink(view: () => Document): Unit =
    "behave like a page with a back link" must {
      "have a back link" in {
        assertRenderedById(view(), "back-link")
      }
    }
}
