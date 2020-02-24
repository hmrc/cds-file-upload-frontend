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

import org.scalatestplus.play.{PlaySpec}

class AppConfigSpec extends PlaySpec {

  //val cfg = app.injector.instanceOf[AppConfig]
  val cfg = pureconfig.loadConfigOrThrow[AppConfig]

  "app config" should {

    "have test-only batch upload endpoint in packaged configuration" in {
      cfg.microservice.services.customsDeclarations.batchUploadEndpoint mustBe "http://localhost:6793/cds-file-upload-service/test-only/batch-file-upload"
    }
  }
}
