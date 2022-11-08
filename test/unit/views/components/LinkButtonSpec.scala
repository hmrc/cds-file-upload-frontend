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

package views.components

import base.{Injector, UnitViewSpec}
import controllers.routes.ChoiceController
import play.twirl.api.Html
import views.html.components.link_button

class LinkButtonSpec extends UnitViewSpec with Injector {

  private val linkPartial = instanceOf[link_button]

  private def createLink(): Html = linkPartial("signOut.link", ChoiceController.onPageLoad)(messages)

  "linkButton" should {

    "display a link with correct href" in {
      val element = createLink().getElementsByClass("govuk-button").first()
      val tag = element.getElementsByTag("a").first()
      tag must haveHref(ChoiceController.onPageLoad.url)
    }
  }
}
