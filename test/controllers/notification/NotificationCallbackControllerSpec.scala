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

package controllers.notification

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class NotificationCallbackControllerSpec extends PlaySpec with GuiceOneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]
  val notificationUrl = s"http://localhost:$port/internal/notification"
  
  "Notification endpoint" should {

    "return 400 Bad Request on POST requests for invalid xml" in {
      val response = await(wsClient.url(notificationUrl).withHeaders("Content-Type" -> "application/xml").post(<invalid/>))

      response.status mustBe BAD_REQUEST
    }

    "return 202 Accepted on POST requests for valid xml" in {
      val notification =
            <Root>
              <FileReference>some-file-ref-123</FileReference>
              <BatchId>5e634e09-77f6-4ff1-b92a-8a9676c715c4</BatchId>
              <FileName>sample.pdf</FileName>
              <Outcome>SUCCESS</Outcome>
              <Details>[detail block]</Details>
            </Root>
      
      val response = await(wsClient.url(notificationUrl).withHeaders("Content-Type" -> "application/xml").post(notification))

      response.status mustBe ACCEPTED
    }
  }
}
