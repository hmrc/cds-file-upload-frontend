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

import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, KebabCase}

case class AppConfig(
  appName: String,
  developerHubClientId: String,
  assets: Assets,
  googleAnalytics: GoogleAnalytics,
  microservice: Microservice,
  fileFormats: FileFormats,
  notifications: Notifications,
  feedback: Feedback,
  proxy: Proxy,
  fileUploadAnswersRepository: FileUploadAnswersRepository,
  secureMessageAnswersRepository: SecureMessageAnswersRepository,
  trackingConsentFrontend: TrackingConsentFrontend,
  platform: Platform,
  allowList: AllowList
)

object AppConfig {
  implicit val appNameHint: ProductHint[AppConfig] = ProductHint(
    (fieldName: String) =>
      fieldName match {
        case "appName" | "developerHubClientId" => fieldName
        case _                                  => KebabCase.fromTokens(CamelCase.toTokens(fieldName))
    }
  )
}

case class Assets(version: String, url: String) {
  lazy val prefix: String = s"$url$version"
}

case class GoogleAnalytics(token: String, host: String)

case class Microservice(services: Services)

case class Services(
  customsDeclarations: CustomsDeclarations,
  cdsFileUploadFrontend: CDSFileUploadFrontend,
  cdsFileUpload: CDSFileUpload,
  keystore: Keystore,
  contactFrontend: ContactFrontend,
  secureMessaging: SecureMessaging
)

case class CustomsDeclarations(protocol: Option[String], host: String, port: Option[Int], batchUploadUri: String, apiVersion: String) {
  def batchUploadEndpoint: String = s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$batchUploadUri"
}

case class CDSFileUploadFrontend(protocol: Option[String], host: String, port: Option[Int]) {
  val uri: String = s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}"
}

case class CDSFileUpload(
  protocol: Option[String],
  host: String,
  port: Option[Int],
  fetchNotificationUri: String,
  fetchDeclarationStatus: String,
  fetchVerifiedEmail: String
) {
  def fetchNotificationEndpoint(reference: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$fetchNotificationUri/$reference"

  def fetchDeclarationStatusEndpoint(mrn: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$fetchDeclarationStatus/$mrn"

  def fetchVerifiedEmailEndpoint(eori: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$fetchVerifiedEmail/$eori"
}

case class Keystore(protocol: String = "https", host: String, port: Int, defaultSource: String, domain: String) {
  lazy val baseUri: String = s"$protocol://$host:$port"
}

case class ContactFrontend(url: String, serviceId: String) {
  lazy val giveFeedbackLink: String = s"$url?service=$serviceId"
}

case class FileFormats(maxFileSizeMb: Int, approvedFileTypes: String, approvedFileExtensions: String)

case class Notifications(maxRetries: Int, retryPauseMillis: Int)

case class Feedback(url: String)

case class Proxy(protocol: String, host: String, port: Int, username: String, password: String, proxyRequiredForThisEnvironment: Boolean)

case class FileUploadAnswersRepository(ttlSeconds: Int)

case class SecureMessageAnswersRepository(ttlSeconds: Int)

case class Gtm(container: String)

case class TrackingConsentFrontend(gtm: Gtm, url: String)

case class Platform(frontend: Frontend)

case class Frontend(host: Option[String])

case class SecureMessaging(
  protocol: Option[String],
  host: String,
  port: Option[Int],
  fetchInbox: String,
  fetchMessage: String,
  replyResult: String,
  submitReply: String
) {
  lazy val fetchInboxEndpoint: String = s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$fetchInbox"

  def fetchMessageEndpoint(client: String, conversationId: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$fetchMessage/$client/$conversationId"

  lazy val replyResultSection2Replace = "CLIENT_ID/CONVERSATION_ID"

  def replyResultEndpoint(client: String, conversationId: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$replyResult"
      .replace(replyResultSection2Replace, s"$client/$conversationId")

  def submitReplyEndpoint(client: String, conversationId: String): String =
    s"${protocol.getOrElse("https")}://$host:${port.getOrElse(443)}$submitReply/$client/$conversationId"
}

case class AllowList(eori: List[String], refererServices: List[String])
