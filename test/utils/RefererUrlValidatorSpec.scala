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

package utils

import base.AppConfigMockHelper.generateMockConfig
import base.Injector
import config.AllowList
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class RefererUrlValidatorSpec extends PlaySpec with Injector with MockitoSugar {

  private val allowedService1 = "service1"
  private val allowedService2 = "service2"
  private val disallowedService3 = "service3"
  private val validRefererUrl = s"/$allowedService1/"

  lazy implicit val appConfig = generateMockConfig(allowList = AllowList(List.empty, List(allowedService1, allowedService2)))

  val validator = RefererUrlValidator

  "RefererUrlValidator" should {
    "allow referer urls that reference the services on the allow list" in {
      validator.isValid(s"/$allowedService1/") mustBe true
      validator.isValid(s"/$allowedService2/") mustBe true
    }

    "deny referer urls that reference the services not on the allow list" in {
      validator.isValid(s"/$disallowedService3/") mustBe false
      validator.isValid(s"/something$allowedService1/") mustBe false
      validator.isValid(s"/something/$allowedService1/") mustBe false
    }

    "deny referer urls that do not start with a '/'" in {
      validator.isValid(s"$allowedService1/") mustBe false
    }

    "deny referer urls that specify a host value" in {
      validator.isValid(s"www.google.com/$allowedService1/") mustBe false
    }

    "deny referer urls that specify a protocol" in {
      validator.isValid(s"javaScript:/$allowedService1/") mustBe false
    }

    "deny referer urls that contain prohibited ASCII characters" in {
      val prohibitedChars =
        Seq('`', '¬', '\'', '!', '"', '£', '$', '%', '^', '*', '(', ')', '+', '[', ']', '{', '}', ';', ':', '@', '#', '~', '<', '>', ',', '.', '\\')

      prohibitedChars.foreach(
        char =>
          withClue(s"including char '${char}'") {
            validator.isValid(s"${validRefererUrl}${char}") mustBe false
        }
      )
    }

    "allow referer urls that contain allowed non-alphanumeric ASCII characters" in {
      val allowedChars = Seq('_', '-', '/', '?', '&', '=')

      allowedChars.foreach(
        char =>
          withClue(s"including char '${char}'") {
            validator.isValid(s"${validRefererUrl}${char}") mustBe true
        }
      )
    }

    "deny referer urls that contain extended characters" in {
      val prohibitedChars = Seq('\u200B', '¥', 'ä')

      prohibitedChars.foreach(
        char =>
          withClue(s"including char '${char}'") {
            validator.isValid(s"${validRefererUrl}${char}") mustBe false
        }
      )
    }
  }
}
