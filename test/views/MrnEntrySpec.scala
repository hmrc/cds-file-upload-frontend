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

import forms.MRNFormProvider
import models.MRN
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import play.twirl.api.Html
import views.behaviours.StringViewBehaviours
import views.html.mrn_entry


class MrnEntrySpec extends DomAssertions with StringViewBehaviours[MRN] with PropertyChecks {

  val messagePrefix = "mrnEntryPage"

  val form = new MRNFormProvider()()

  def view = mrn_entry(form)(fakeRequest, messages, appConfig)

  def createViewUsingForm: Form[MRN] => Html = form => mrn_entry(form)(fakeRequest, messages, appConfig)

  "MRN Entry Page" must {
    behave like normalPage(view, messagePrefix)
    behave like stringPage(
      createViewUsingForm,
      "value",
      messagePrefix,
      List("mrnEntryPage.hint1", "mrnEntryPage.hint2"))
  }
}

