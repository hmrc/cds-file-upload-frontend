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

package base

import config._
import org.mockito.MockitoSugar.mock

object AppConfigMockHelper {
  def generateMockConfig(
    appName: String = "",
    developerHubClientId: String = "",
    assets: Assets = mock[Assets],
    googleAnalytics: GoogleAnalytics = mock[GoogleAnalytics],
    microservice: Microservice = mock[Microservice],
    fileFormats: FileFormats = mock[FileFormats],
    notifications: Notifications = mock[Notifications],
    feedback: Feedback = mock[Feedback],
    proxy: Proxy = mock[Proxy],
    answersRepository: FileUploadAnswersRepository = mock[FileUploadAnswersRepository],
    secureMessageAnswersRepository: SecureMessageAnswersRepository = mock[SecureMessageAnswersRepository],
    trackingConsentFrontend: TrackingConsentFrontend = mock[TrackingConsentFrontend],
    play: Play = mock[Play],
    allowList: AllowList = mock[AllowList]
  ) = AppConfig(
    appName,
    developerHubClientId,
    assets,
    googleAnalytics,
    microservice,
    fileFormats,
    notifications,
    feedback,
    proxy,
    answersRepository,
    secureMessageAnswersRepository,
    trackingConsentFrontend,
    play,
    allowList
  )

  def generateMockServices(
    customsDeclarations: CustomsDeclarations = mock[CustomsDeclarations],
    cdsFileUploadFrontend: CDSFileUploadFrontend = mock[CDSFileUploadFrontend],
    cdsFileUpload: CDSFileUpload = mock[CDSFileUpload],
    keystore: Keystore = mock[Keystore],
    contactFrontend: ContactFrontend = mock[ContactFrontend],
    secureMessaging: SecureMessaging = mock[SecureMessaging]
  ) = Services(customsDeclarations, cdsFileUploadFrontend, cdsFileUpload, keystore, contactFrontend, secureMessaging)
}
