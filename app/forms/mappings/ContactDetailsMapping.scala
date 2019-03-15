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

package forms.mappings

import com.google.inject.Inject
import forms.FormErrorHelper
import models.ContactDetails
import play.api.data.Forms.of
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms._
import play.api.data.Mapping

import scala.util.Try
import scala.util.control.Exception.allCatch

object ContactDetailsMapping extends FormErrorHelper with Mappings {

  val contactDetailsMapping = mapping(
    "name" -> text().verifying(maxLength(35, "Name must be less than or equal to 35 characters")),
    "companyName" -> text().verifying(maxLength(35, "Company name must be less than or equal to 35 characters")),
    "phoneNumber" -> text().verifying(maxLength(35, "Phone number must be less than or equal to 35 characters")),
    "email" -> text().verifying(maxLength(35, "Email must be less than or equal to 35 characters"))
  )(ContactDetails.apply)(ContactDetails.unapply)

}
