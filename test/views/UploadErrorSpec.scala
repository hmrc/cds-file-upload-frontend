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

import models.requests.{AuthenticatedRequest, SignedInUser}
import utils.FakeRequestCSRFSupport._
import views.behaviours.ViewBehaviours
import views.html.upload_error
import views.matchers.ViewMatchers

class UploadErrorSpec extends ViewBehaviours with ViewMatchers {

  val page = instanceOf[upload_error]

  val view = () => page()(fakeRequest.withCSRFToken, messages)

  val messageKeyPrefix = "uploadError"

  "Upload Error page" should {

    behave like normalPage(view, messageKeyPrefix)

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page()(AuthenticatedRequest(fakeRequest.withCSRFToken, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "have a paragraph" in {
      val paragraph = asDocument(view()).getElementsByTag("p").get(1)

      paragraph must containMessage("uploadError.paragraph")
    }

    "have a bullet list" in {
      val bulletList = asDocument(view()).getElementsByClass("list list-bullet").first()

      bulletList must containMessage("uploadError.bullet.1")
      bulletList must containMessage("uploadError.bullet.2")
      bulletList must containMessage("uploadError.bullet.3")
    }

    "have a button that links to start page" in {
      val button = asDocument(view()).getElementsByClass("govuk-button").first()

      button must containMessage("uploadError.button")
      button must haveHref(controllers.routes.StartController.displayStartPage())
    }

    "have a guidance" in {
      val guidance = asDocument(view()).getElementsByTag("p").get(2)

      guidance must containMessage("uploadError.guidance")
    }
  }

}
