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

package config

import org.scalatestplus.play.PlaySpec
import pureconfig.ConfigSource
import pureconfig.generic.auto._

class AppConfigSpec extends PlaySpec {

  val config = ConfigSource.default.loadOrThrow[AppConfig]

  "App Config" should {

    "have test-only batch upload endpoint in packaged configuration" in {

      val customsDeclarations = config.microservice.services.customsDeclarations

      val expectedUrl = "http://localhost:6793/cds-file-upload-service/test-only/batch-file-upload"

      customsDeclarations.batchUploadEndpoint mustBe expectedUrl
    }

    "have a correct configuration for CDS File Upload" in {
      val cdsFileUpload = config.microservice.services.cdsFileUpload

      cdsFileUpload.fetchNotificationEndpoint("reference") mustBe "http://localhost:6795/cds-file-upload/notification/reference"
      cdsFileUpload.fetchDeclarationStatusEndpoint("sampleMrn") mustBe "http://localhost:6795/cds-file-upload/declaration-information/sampleMrn"
      cdsFileUpload.fetchVerifiedEmailEndpoint("sampleEori") mustBe "http://localhost:6795/cds-file-upload/eori-email/sampleEori"
    }

    "have gtm container" in {

      config.trackingConsentFrontend.gtm.container mustBe "a"
    }

    "have tracking-consent-frontend url" in {

      val expectedUrl = "http://localhost:12345/tracking-consent/tracking.js"

      config.trackingConsentFrontend.url mustBe expectedUrl
    }

    "have link to contact-frontend" in {

      val expectedUrl = "http://localhost:9250/contact/beta-feedback-unauthenticated?service=SFUS"

      config.microservice.services.contactFrontend.giveFeedbackLink mustBe expectedUrl
    }

    "have a correct configuration for secure-message-frontend" in {

      val secureMessaging = config.microservice.services.secureMessaging

      secureMessaging.fetchInboxEndpoint mustBe "http://localhost:9055/secure-message-frontend/cds-file-upload-service/messages"
    }
  }
}
