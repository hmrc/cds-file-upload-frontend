/*
 * Copyright 2020 HM Revenue & Customs
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

import pureconfig.{CamelCase, ConfigFieldMapping, KebabCase}
import pureconfig.generic.ProductHint

case class AppConfig(
                      appName: String,
                      developerHubClientId: String,
                      contactFrontend: ContactFrontend,
                      assets: Assets,
                      googleAnalytics: GoogleAnalytics,
                      microservice: Microservice,
                      fileFormats: FileFormats,
                      notifications: Notifications,
                      feedback: Feedback,
                      proxy: Proxy,
                      accessibilityLinkUrl: String)

object AppConfig {
  implicit val appNameHint: ProductHint[AppConfig] = ProductHint(new ConfigFieldMapping {
    def apply(fieldName: String): String = fieldName match {
      case "appName" | "developerHubClientId" => fieldName
      case _ => KebabCase.fromTokens(CamelCase.toTokens(fieldName))
    }
  })
}

case class ContactFrontend(host: String, serviceIdentifier: String = "MyService") {
  lazy val reportAProblemPartialUrl = s"$host/contact/problem_reports_ajax?service=$serviceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$host/contact/problem_reports_nonjs?service=$serviceIdentifier"
}

case class Assets(version: String, url: String) {
  lazy val prefix: String = s"$url$version"
}

case class GoogleAnalytics(token: String, host: String)

case class Microservice(services: Services)

case class Services(customsDeclarations: CustomsDeclarations, cdsFileUploadFrontend: CDSFileUploadFrontend, keystore: Keystore)

case class CustomsDeclarations(protocol: Option[String], host: String, port: Option[Int], batchUploadUri: String, apiVersion: String) {
  def batchUploadEndpoint: String = s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$batchUploadUri"
}


case class CDSFileUploadFrontend(protocol: Option[String], host: String, port: Option[Int]) {
  val uri: String = s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}"
}

case class Keystore(protocol: String = "https", host: String, port: Int, defaultSource: String, domain: String) {
  lazy val baseUri: String = s"$protocol://$host:$port"
}

case class FileFormats(maxFileSizeMb: Int, approvedFileTypes: String, approvedFileExtensions: String)

case class Notifications(authToken: String, maxRetries: Int, retryPauseMillis: Int, ttlSeconds: Int)

case class Feedback(url: String)

case class Proxy(protocol:String, host:String, port:Int,username:String,password:String,proxyRequiredForThisEnvironment:Boolean)
