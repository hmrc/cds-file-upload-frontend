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

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration

class SecureMessagingConfigSpec extends PlaySpec {

  private val configWithSecureMessagingEnabled: Configuration =
    Configuration(ConfigFactory.parseString("microservice.services.features.secureMessaging=enabled"))
  private val configWithSecureMessagingDisabled: Configuration =
    Configuration(ConfigFactory.parseString("microservice.services.features.secureMessaging=disabled"))
  private val emptyConfig: Configuration =
    Configuration(ConfigFactory.parseString("microservice.services.features.default=disabled"))

  private def secureMessagingConfig(configuration: Configuration) = new SecureMessagingConfig(new FeatureSwitchConfig(configuration))

  "SecureMessagingConfig on isSecureMessagingEnabled" should {

    "return true" when {
      "the feature is enabled" in {
        secureMessagingConfig(configWithSecureMessagingEnabled).isSecureMessagingEnabled mustBe true
      }
    }

    "return false" when {
      "the feature is disabled" in {
        secureMessagingConfig(configWithSecureMessagingDisabled).isSecureMessagingEnabled mustBe false
      }

      "there is no config for the feature" in {
        secureMessagingConfig(emptyConfig).isSecureMessagingEnabled mustBe false
      }
    }
  }
}
