/*
 * Copyright 2024 HM Revenue & Customs
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

import base.UnitSpec

class ServiceUrlsSpec extends UnitSpec {

  val serviceUrls = instanceOf[ServiceUrls]

  "have a correct configuration for Registration url" in {
    serviceUrls.cdsRegister mustBe "https://www.gov.uk/guidance/get-access-to-the-customs-declaration-service"
  }

  "have a correct configuration for Check Status url" in {
    serviceUrls.cdsSubscribe mustBe "http://localhost:6750/customs-enrolment-services/cds/subscribe"
  }

  "have a correct configuration for Customs Email Frontend url" in {
    serviceUrls.emailFrontend mustBe "http://localhost:9898/manage-email-cds/service/cds-file-upload"
  }

  "have a correct configuration for EORI service url" in {
    serviceUrls.eoriService mustBe "https://www.gov.uk/eori"
  }

  "have a correct configuration for Feedback url" in {
    serviceUrls.feedbackFrontend mustBe "http://locahost:9514/feedback/cds-file-upload-frontend"
  }

  "have a correct configuration for the GovUk url" in {
    serviceUrls.govUk mustBe "https://www.gov.uk"
  }

  "have a correct configuration for the Auth-page url" in {
    serviceUrls.login mustBe "http://localhost:9949/auth-login-stub/gg-sign-in"
  }

  "have a correct configuration for the SFUS start-page url" in {
    serviceUrls.loginContinue mustBe "http://localhost:6793/cds-file-upload-service/start"
  }

  "have a correct configuration for National Clearance Hub url" in {
    serviceUrls.nationalClearingHub mustBe "mailto:nch.cds@hmrc.gov.uk"
  }
}
