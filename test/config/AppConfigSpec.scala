/*
 * Copyright 2022 HM Revenue & Customs
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

  val config =
    ConfigSource.string("""
      |appName="cds-file-upload-frontend"
      |developerHubClientId="cds-file-upload-frontend"
      |
      |google-analytics {
      |  token=N/A
      |  host=auto
      |}
      |
      |microservice {
      |  metrics {
      |    graphite {
      |      host = localhost
      |      port = 2003
      |      prefix = play.appName
      |      enabled = false
      |    }
      |  }
      |
      |  services {
      |    auth {
      |      host = localhost
      |      port = 8500
      |    }
      |
      |    keystore {
      |      protocol = http
      |      host = localhost
      |      port = 8400
      |      default-source = appName
      |      domain = keystore
      |    }
      |
      |    customs-declarations {
      |      protocol = http
      |      host = localhost
      |      port = 6793
      |      batch-upload-uri = /cds-file-upload-service/test-only/batch-file-upload
      |      api-version = "3.0"
      |    }
      |
      |    cds-file-upload-frontend {
      |      protocol = http
      |      host = localhost
      |      port = 6793
      |    }
      |
      |    cds-file-upload {
      |      protocol = http
      |      host = localhost
      |      port = 6795
      |      fetch-notification-uri = /cds-file-upload/notification
      |      fetch-verified-email = /cds-file-upload/eori-email
      |    }
      |
      |    contact-frontend {
      |      url = "http://localhost:9250/contact/beta-feedback-unauthenticated"
      |      service-id = "SFUS"
      |    }
      |
      |    secure-messaging {
      |      protocol = http
      |      host = localhost
      |      port = 9055
      |      fetch-inbox = /secure-message-frontend/cds-file-upload-service/messages
      |      fetch-message = /secure-message-frontend/cds-file-upload-service/conversation
      |      reply-result = /secure-message-frontend/cds-file-upload-service/conversation/CLIENT_ID/CONVERSATION_ID/result
      |      submit-reply = /secure-message-frontend/cds-file-upload-service/conversation
      |    }
      |
      |    features {
      |      default = disabled
      |    }
      |  }
      |}
      |
      |proxy {
      |  protocol = https
      |  host = outbound-proxy-vip
      |  port = 3128
      |  username = cds-file-upload-frontend
      |  password = na
      |  proxy-required-for-this-environment = false
      |}
      |
      |assets {
      |  version = "3.7.0"
      |  version = "1.0.0"
      |  url = "http://localhost:9032/assets/"
      |}
      |
      |file-formats {
      |  max-file-size-mb = 10
      |  approved-file-extensions = ".jpeg,.jpg,.png,.pdf,.txt"
      |  approved-file-types = "image/jpeg,image/png,application/pdf,text/plain"
      |}
      |
      |# ttl cannot be changed after initial deployment without manually dropping the index
      |notifications {
      |  max-retries = 1000
      |  retry-pause-millis = 250
      |}
      |
      |feedback {
      |  url = "http://localhost:9514/feedback/FUS"
      |}
      |
      |accessibility-statement.service-path = "/cds-file-upload"
      |
      |urls {
      |  login = "http://localhost:9949/auth-login-stub/gg-sign-in"
      |  loginContinue = "http://localhost:6793/cds-file-upload-service/start"
      |  eoriService = "https://www.gov.uk/eori"
      |  cdsRegister = "https://www.gov.uk/guidance/get-access-to-the-customs-declaration-service"
      |  cdsCheckStatus = "https://www.tax.service.gov.uk/customs/register-for-cds/are-you-based-in-uk"
      |  feedbackFrontend = "http://locahost:9514/feedback/cds-file-upload-frontend"
      |  nationalClearingHubLink = "mailto:nch.cds@hmrc.gov.uk"
      |  emailFrontendUrl = "http://localhost:9898/manage-email-cds/service/cds-file-upload"
      |}
      |
      |file-upload-answers-repository {
      |  ttl-seconds = 3600
      |}
      |
      |secure-message-answers-repository {
      |  ttl-seconds = 3600
      |}
      |
      |allow-list {
      |  eori = [123456,345678]
      |  referer-services = ["cds-file-upload-service", "customs-declare-exports"]
      |}
      |
      |tracking-consent-frontend {
      |  gtm.container = "a"
      |  host = "http://localhost:12345"
      |}
      |
      |# Default value for local environment
      |play.frontend.host = "http://localhost:6793"
      |
    """.stripMargin).loadOrThrow[AppConfig]

  "App Config" should {

    "have test-only batch upload endpoint in packaged configuration" in {
      val customsDeclarations = config.microservice.services.customsDeclarations
      val expectedUrl = "http://localhost:6793/cds-file-upload-service/test-only/batch-file-upload"
      customsDeclarations.batchUploadEndpoint mustBe expectedUrl
    }

    "have a correct configuration for CDS File Upload" in {
      val cdsFileUpload = config.microservice.services.cdsFileUpload
      cdsFileUpload.fetchNotificationEndpoint("reference") mustBe "http://localhost:6795/cds-file-upload/notification/reference"
      cdsFileUpload.fetchVerifiedEmailEndpoint("sampleEori") mustBe "http://localhost:6795/cds-file-upload/eori-email/sampleEori"
    }

    "have gtm container" in {
      config.trackingConsentFrontend.gtm.container mustBe "a"
    }

    "have tracking-consent-frontend url" in {
      val expectedUrl = "http://localhost:12345"
      config.trackingConsentFrontend.host mustBe expectedUrl
    }

    "have link to contact-frontend" in {
      val expectedUrl = "http://localhost:9250/contact/beta-feedback-unauthenticated?service=SFUS"
      config.microservice.services.contactFrontend.giveFeedbackLink mustBe expectedUrl
    }

    "have a correct configuration for secure-message-frontend" in {
      val secureMessaging = config.microservice.services.secureMessaging

      secureMessaging.fetchInboxEndpoint mustBe "http://localhost:9055/secure-message-frontend/cds-file-upload-service/messages"
      secureMessaging.fetchMessageEndpoint("client", "conversation-id") mustBe
        "http://localhost:9055/secure-message-frontend/cds-file-upload-service/conversation/client/conversation-id"

      secureMessaging.replyResultEndpoint("client", "conversationId") mustBe
        "http://localhost:9055/secure-message-frontend/cds-file-upload-service/conversation/client/conversationId/result"

      secureMessaging.replyResultEndpoint("client", "conversationId") mustBe
        "http://localhost:9055/secure-message-frontend/cds-file-upload-service/conversation/client/conversationId/result"
    }

    "have a TTL defined for the FileUploadAnswersRepository" in {
      config.fileUploadAnswersRepository.ttlSeconds mustBe 3600
    }

    "have a TTL defined for the SecureMessageAnswersRepository" in {
      config.secureMessageAnswersRepository.ttlSeconds mustBe 3600
    }

    "have an allow list eori value defined" in {
      config.allowList.eori.size mustBe 2
    }

    "have an allow list refererServices value defined" in {
      config.allowList.refererServices.size mustBe 2
    }
  }
}
