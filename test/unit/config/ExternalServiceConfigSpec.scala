/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import base.SpecBase

class ExternalServiceConfigSpec extends SpecBase {
  val config = instanceOf[ExternalServicesConfig]

  "have a correct configuration for EORI service url" in {
    config.eoriService mustBe "https://www.gov.uk/eori"
  }

  "have a correct configuration for Registration url" in {
    config.cdsRegister mustBe "https://www.gov.uk/guidance/get-access-to-the-customs-declaration-service"
  }

  "have a correct configuration for Check Status url" in {
    config.cdsCheckStatus mustBe "https://www.tax.service.gov.uk/customs/register-for-cds/are-you-based-in-uk"
  }

  "have a correct configuration for Feedback url" in {
    config.feedbackFrontend mustBe "http://locahost:9514/feedback/cds-file-upload-frontend"
  }

  "have a correct configuration for National Clearance Hub url" in {
    config.nationalClearingHubLink mustBe "mailto:nch.cds@hmrc.gov.uk"
  }

  "have a correct configuration for Customs Email Frontend url" in {
    config.emailFrontendUrl mustBe "http://localhost:9898/manage-email-cds/service/cds-file-upload"
  }
}
