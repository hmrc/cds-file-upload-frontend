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

import java.io.IOException

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.NotificationService

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class NotificationCallbackControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures {
  
  val mockNotificationService = mock[NotificationService]
  val controller = new NotificationCallbackController(mockNotificationService)
  
  "NotificationCallbackController" should {
    "return internal server error when there is a downstream failure" in {
      
      when(mockNotificationService.save(any[NodeSeq])(any[ExecutionContext])).thenReturn(Future.successful(Left(new IOException("Server error"))))
      
      val result = controller.onNotify()(FakeRequest("", "").withBody(<notification/>))
      
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
