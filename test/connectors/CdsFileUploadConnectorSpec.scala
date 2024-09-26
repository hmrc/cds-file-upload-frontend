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

package connectors

import models.{EORI, Email, Notification}
import org.mockito.MockitoSugar.when
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class CdsFileUploadConnectorSpec extends ConnectorSpec {

  val connector = new CdsFileUploadConnector(appConfig, httpClient)(global)

  "CdsFileUploadConnector on getNotification" should {

    "return notification" when {
      "notification exists" in {
        val notification = Notification("fileReference", "outcome", Some("fileName"))
        when(execute[Option[Notification]]).thenReturn(Future.successful(Some(notification)))

        val result = connector.getNotification("fileReference").futureValue
        result mustBe Some(notification)
      }
    }

    "return None" when {
      "there is no notification with specific reference" in {
        when(execute[Option[Notification]]).thenReturn(Future.successful(None))

        val result = connector.getNotification("fileReference").futureValue
        result mustBe None
      }
    }
  }

  "CdsFileUploadConnector on getVerifiedEmail" should {
    lazy val sampleEori = EORI("12345")

    "handle a 200 response by returning a Email" in {
      val expectedEmail = Email("some@email.com", true)
      when(execute[Option[Email]]).thenReturn(Future.successful(Some(expectedEmail)))

      val result = connector.getVerifiedEmailAddress(sampleEori).futureValue
      result mustBe Some(expectedEmail)
    }

    "handle a 404 response by returning None" in {
      when(execute[Option[Email]]).thenReturn(Future.successful(None))

      val result = connector.getVerifiedEmailAddress(sampleEori).futureValue
      result mustBe None
    }

    "handle a 'non 404' 4XX response by throwing an exception" in {
      when(execute[Option[Email]]).thenReturn(Future.failed(UpstreamErrorResponse("", 410)))

      val result = connector.getVerifiedEmailAddress(sampleEori)
      assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
    }

    "handle a 5XX response by throwing an exception" ignore {
      when(execute[Option[Email]]).thenReturn(Future.failed(UpstreamErrorResponse("", 500)))

      val result = connector.getVerifiedEmailAddress(sampleEori)
      assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
    }
  }
}
