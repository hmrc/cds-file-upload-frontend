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

import controllers.{Position, First, Middle, Last}
import models.UploadRequest
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.upload_your_files

class UploadYourFilesSpec extends ViewSpecBase with ViewBehaviours {

  def view(pos: Position): Html =
    upload_your_files(
      new UploadRequest("", Map("" -> "")), "", pos)(fakeRequest, messages, appConfig)

  val view: () => Html = () => view(First)

  val messagePrefix = "fileUploadPage"

  val messageKeys = List(
                          "p.fileNeedsToBe",
                          "listItem1",
                          "listItem2",
                          "listItem3",
                          "listItem4",
                          "listItem5",
                          "listItem6"
                        )

  "Upload your files page" must {

    behave like pageWithoutHeading(view, messagePrefix, messageKeys: _*)

    "show heading for first file" in {

      assertPageTitleEqualsMessage(asDocument(view(First)), s"$messagePrefix.heading.first")
    }

    "show heading for middle file" in {

      assertPageTitleEqualsMessage(asDocument(view(Middle)), s"$messagePrefix.heading.middle")
    }

    "show heading for last file" in {

      assertPageTitleEqualsMessage(asDocument(view(Last)), s"$messagePrefix.heading.last")
    }
  }

}
