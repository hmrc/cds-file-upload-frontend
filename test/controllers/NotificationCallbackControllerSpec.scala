/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class NotificationCallbackControllerSpec extends PlaySpec with GuiceOneServerPerSuite {

  "Dummy notification endpoint" should {

    "return 202 on POST requests" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val notificationUrl = s"http://localhost:$port/cds-file-upload-service/notification"

      val response = await(wsClient.url(notificationUrl).withHeaders("Content-Type" -> "application/xml").post("<foo/>"))

      response.status mustBe ACCEPTED
    }
  }
}
