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

package services

import connectors.MongoCacheConnector
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class NotificationServiceSpec extends WordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc = mock[HeaderCarrier]
  val mockCache = mock[MongoCacheConnector]

  val service = new NotificationService(mockCache)

  val SuccessNotification =
    <Root>
      <FileReference>e4d94295-52b1-4837-bdc0-7ab8d7b0f1af</FileReference>
      <BatchId>5e634e09-77f6-4ff1-b92a-8a9676c715c4</BatchId>
      <FileName>sample.pdf</FileName>
      <Outcome>SUCCESS</Outcome>
      <Details>[detail block]</Details>
    </Root>

  "Notification service" should {

    "save a success notification" in {
      when(mockCache.save(any[CacheMap])(any[HeaderCarrier])).thenAnswer(new Answer[Future[CacheMap]]{
        override def answer(invocation: InvocationOnMock): Future[CacheMap] = Future.successful(invocation.getArgument(0))
      })

      service.save(SuccessNotification).futureValue shouldBe CacheMap("e4d94295-52b1-4837-bdc0-7ab8d7b0f1af", Map("outcome" -> JsString("SUCCESS")))
    }
  }
}
