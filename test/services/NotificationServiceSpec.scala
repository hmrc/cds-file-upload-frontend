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

import java.io.IOException

import models.Notification
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import reactivemongo.api.commands.DefaultWriteResult
import repositories.NotificationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class NotificationServiceSpec extends PlaySpec with MockitoSugar {

  implicit val hc = mock[HeaderCarrier]
  val mockRepository = mock[NotificationRepository]

  val service = new NotificationService(mockRepository)

  val SuccessNotification =
    <Root>
      <FileReference>e4d94295-52b1-4837-bdc0-7ab8d7b0f1af</FileReference>
      <BatchId>5e634e09-77f6-4ff1-b92a-8a9676c715c4</BatchId>
      <FileName>sample.pdf</FileName>
      <Outcome>SUCCESS</Outcome>
      <Details>[detail block]</Details>
    </Root>

  "Notification service" should {

    "save a success notification with timestamp for TTL" in {
      when(mockRepository.insert(any[Notification])(any[ExecutionContext])).thenReturn(Future.successful(DefaultWriteResult(ok = true, 1, Seq.empty, None, None, None)))

      await(service.save(SuccessNotification))
      
      val captor: ArgumentCaptor[Notification] = ArgumentCaptor.forClass(classOf[Notification])
      verify(mockRepository).insert(captor.capture())(any[ExecutionContext])
      val notification = captor.getValue
      notification.fileReference mustBe "e4d94295-52b1-4837-bdc0-7ab8d7b0f1af"
      notification.outcome mustBe "SUCCESS"
      notification.createdAt.withTimeAtStartOfDay() mustBe DateTimeUtils.now.withTimeAtStartOfDay()  
    }
    
    "return an exception when insert fails" in {
      val exception = new IOException("downstream failure")
      when(mockRepository.insert(any[Notification])(any[ExecutionContext])).thenReturn(Future.failed(exception))
      
      val result = await(service.save(SuccessNotification))
      
      result mustBe Left(exception)
    }
  }
}
