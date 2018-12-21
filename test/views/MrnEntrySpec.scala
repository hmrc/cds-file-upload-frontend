/*
 * Copyright 2018 HM Revenue & Customs
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

import domain.MRN
import forms.MRNFormProvider
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import views.behaviours.StringViewBehaviours
import views.html.mrn_entry


class MrnEntrySpec extends ViewSpecBase with StringViewBehaviours[MRN] with PropertyChecks {

  val form = new MRNFormProvider()()

  val view: () => Html = () => mrn_entry(form)(fakeRequest, messages, appConfig)

  val messagePrefix = "mrnEntryPage"

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => mrn_entry(form)(fakeRequest, messages, appConfig)

  "MRN Entry Page" must {
    behave like normalPage(view, messagePrefix)
    behave like stringPage(createViewUsingForm, "value", messagePrefix)
  }
}

