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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class ServiceUrls @Inject() (val configuration: Configuration) {

  private def loadUrl(key: String): String =
    configuration.getOptional[String](s"urls.$key").getOrElse(throw new Exception(s"Missing configuration key: urls.$key"))

  val cdsRegister: String = loadUrl("cdsRegister")
  val cdsSubscribe: String = loadUrl("cdsSubscribe")
  val emailFrontend: String = loadUrl("emailFrontend")
  val eoriService: String = loadUrl("eoriService")
  val feedbackFrontend: String = loadUrl("feedbackFrontend")
  val govUk: String = loadUrl("govUk")
  val login: String = loadUrl("login")
  val loginContinue: String = loadUrl("loginContinue")
  val nationalClearingHub: String = loadUrl("nationalClearingHub")
}
