/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.views

import base.SpecBase
import views.html.undeliverable_email
import views.matchers.ViewMatchers

import scala.jdk.CollectionConverters._

class UndeliverableEmailSpec extends SpecBase with ViewMatchers {

  lazy val redirectUrl = "/some/url"

  val page = instanceOf[undeliverable_email]

  val view = () => page(redirectUrl)(fakeRequest, messages)

  val messageKeyPrefix = "emailUndeliverable"

  "Unverified Email Page" must {

    "display page header" in {
      view().getElementsByTag("h1").first() must containMessage(s"$messageKeyPrefix.heading")
    }

    "have a button with correct link" in {
      val link = view().getElementsByClass("govuk-button").first()

      link must haveHref(redirectUrl)
      link must containMessage(s"${messageKeyPrefix}.link-text")
    }

    "have paragraph1 with text" in {
      view().getElementById("emailUndeliverable.para1") must containMessage(s"${messageKeyPrefix}.p1")
      view().getElementById("emailUndeliverable.para2") must containMessage(s"${messageKeyPrefix}.p2")
      view().getElementById("emailUndeliverable.para3") must containMessage(s"${messageKeyPrefix}.p3")
    }

    "have bullet list with expected items" in {
      val ul = view().getElementById("emailUnverified.bullets")

      val expectedBulletTextKeys = (1 to 4).map { idx =>
        s"${messageKeyPrefix}.list.${idx}"
      }

      val itemsWithExpectedKeys = ul.children().asScala.zip(expectedBulletTextKeys)

      itemsWithExpectedKeys.foreach { itemWithExpected =>
        val (item, expectedKey) = itemWithExpected
        item must containMessage(expectedKey)
      }
    }
  }
}
