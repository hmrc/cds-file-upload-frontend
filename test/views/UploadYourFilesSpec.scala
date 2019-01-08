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

import models.UploadRequest
import org.scalatest.prop.PropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.{how_many_files_upload, upload_your_files}

class UploadYourFilesSpec extends ViewSpecBase with ViewBehaviours {

  val view: () => Html = () => upload_your_files(
    new UploadRequest("", Map("" -> "")), "")(fakeRequest, messages, appConfig)

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

    behave like normalPage(view, messagePrefix, messageKeys: _*)

  }

}
