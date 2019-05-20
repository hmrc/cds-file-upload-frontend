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

import forms.mappings.ContactDetailsMapping._
import generators.Generators
import models.ContactDetails
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import play.twirl.api.Html
import views.behaviours.{StringViewBehaviours, ViewBehaviours}
import views.html.components.input_text
import views.html.contact_details

class ContactDetailsSpec extends DomAssertions with ViewBehaviours with PropertyChecks with Generators {

  val form: Form[ContactDetails] = Form(contactDetailsMapping)

  val simpleView: () => Html = () => contact_details(form)(fakeRequest, messages, appConfig)

  def view(form: Form[ContactDetails] = form): String = contact_details(form)(fakeRequest, messages, appConfig).toString()

  val messagePrefix = "contactDetails"
  
  def getMessage(key: String): String = messages(s"$messagePrefix.$key")

  "Contact details page" must {

    behave like normalPage(simpleView, messagePrefix)

    "display name input" in {

      forAll { contactDetails: ContactDetails =>

        val popForm = form.fillAndValidate(contactDetails)
        val input = input_text(popForm("name"), getMessage("name")).toString()

        view(popForm) must include(input)
      }
    }

    "display company name input" in {

      forAll { contactDetails: ContactDetails =>

        val popForm = form.fillAndValidate(contactDetails)
        val input = input_text(popForm("companyName"), getMessage("companyName")).toString()

        view(popForm) must include(input)
      }
    }

    "display phone number input" in {

      forAll { contactDetails: ContactDetails =>

        val popForm = form.fillAndValidate(contactDetails)
        val input = input_text(popForm("phoneNumber"), getMessage("phoneNumber")).toString()

        view(popForm) must include(input)
      }
    }

    "display email input" in {

      forAll { contactDetails: ContactDetails =>

        val popForm = form.fillAndValidate(contactDetails)
        val input = input_text(popForm("email"), getMessage("email")).toString()

        view(popForm) must include(input)
      }
    }
  }
}
