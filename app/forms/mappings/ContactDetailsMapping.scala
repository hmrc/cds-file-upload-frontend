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

package forms.mappings

import models.ContactDetails
import play.api.data.Forms._

object ContactDetailsMapping extends Mappings {

  val contactDetailsMapping = mapping(
    "name" -> text().verifying(maxLength(35, "contactDetails.name.invalid")),
    "companyName" -> text().verifying(maxLength(35, "contactDetails.companyName.invalid")),
    "phoneNumber" -> text()
      .verifying(maxLength(15, "contactDetails.phoneNumber.invalid"))
      .verifying("contactDetails.phoneNumber.invalidPattern", isValidPhoneNumber),
    "email" -> text()
      .verifying(maxLength(50, "contactDetails.email.invalid"))
      .verifying("contactDetails.email.invalidPattern", isValidEmail)
  )(ContactDetails.apply)(ContactDetails.unapply)
}
