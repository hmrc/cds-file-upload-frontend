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

package views.behaviours

import play.twirl.api.HtmlFormat
import views.DomAssertions

trait ViewBehaviours extends DomAssertions {

  def normalPage(view: () => HtmlFormat.Appendable, messageKeyPrefix: String, expectedGuidanceKeys: String*) = {

    "behave like a page with a heading" when {
      "rendered" must {

        "display the correct page title" in {
          val doc = asDocument(view())
          assertH1EqualsMessage(doc, s"$messageKeyPrefix.heading")
        }
      }
    }

    "behave like a page with a title" when {
      "rendered" must {

        "display the correct browser title" in {
          val doc = asDocument(view())
          assertEqualsMessage(doc, "title", s"$messageKeyPrefix.heading")
        }
      }
    }

    behave like pageWithoutHeading(view, messageKeyPrefix, expectedGuidanceKeys: _*)
  }

  def pageWithoutHeading(view: () => HtmlFormat.Appendable, messageKeyPrefix: String, expectedGuidanceKeys: String*) =
    "behave like a normal page" when {
      "rendered" must {
        "have the correct banner title" in {
          val doc = asDocument(view())

          val oldNav = doc.getElementById("proposition-menu")
          val newNav = doc.select(".govuk-header__link--service-name")

          val element = if (oldNav != null) oldNav.children.first else newNav.first()
          element.text mustBe messages("common.service.name")
        }

        "display the correct guidance" in {
          val doc = asDocument(view())
          for (key <- expectedGuidanceKeys) assertContainsText(doc, messages(s"$messageKeyPrefix.$key"))
        }
      }
    }

  def pageWithBackLink(view: () => HtmlFormat.Appendable) =
    "behave like a page with a back link" must {
      "have a back link" in {
        val doc = asDocument(view())
        assertRenderedById(doc, "back-link")
      }
    }
}
