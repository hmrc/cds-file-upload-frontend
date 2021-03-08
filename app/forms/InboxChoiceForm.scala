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

package forms

import forms.mappings.EnhancedMapping.requiredRadio
import forms.validators.FieldValidator
import models.{ExportMessages, ImportMessages}
import play.api.data.{Form, Forms}

case class InboxChoiceForm(choice: String)

object InboxChoiceForm {

  val InboxChoiceKey = "choice"

  object Values {
    val ExportsMessages = ExportMessages.toString
    val ImportsMessages = ImportMessages.toString
  }

  val form: Form[InboxChoiceForm] = {
    val inboxChoiceValues = Set(Values.ExportsMessages, Values.ImportsMessages)

    val fieldMapping =
      requiredRadio("inboxChoice.input.error.empty")
        .verifying("inboxChoice.input.error.incorrect", FieldValidator.isContainedIn(inboxChoiceValues))

    val inboxChoiceMapping = Forms.mapping(InboxChoiceKey -> fieldMapping)(InboxChoiceForm.apply)(InboxChoiceForm.unapply)

    Form(inboxChoiceMapping)
  }
}
