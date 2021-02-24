/*
 * Copyright 2021 HM Revenue & Customs
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

import base.{Injector, UnitSpec}
import config.AppConfig
import models.InboxPartial
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach
import play.mvc.Http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnectorSpec extends UnitSpec with BeforeAndAfterEach with Injector with ScalaFutures {
  val appConfig = instanceOf[AppConfig]
  val httpClient = mock[HttpClient]
  implicit val hc = mock[HeaderCarrier]
  implicit val ec: ExecutionContext = ExecutionContext.global

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient)
  }

  val connector = new SecureMessageFrontendConnector(httpClient, appConfig)

  "SecureMessageFrontend" when {
    "retrieveConversationsPartial is called" which {
      "receives a 200 response" should {
        "return a populated InboxPartial" ignore { //TODO: reinstate when connector talks to real secure-message-frontend service
          val partialContent = "<html>Some Content</html>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)

          when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveInboxPartial().futureValue

          result mustBe InboxPartial(partialContent)
        }
      }

      "receives a non 200 response" should {
        "return a failed Future" ignore { //TODO: reinstate when connector talks to real secure-message-frontend service
          val httpResponse = HttpResponse(status = NOT_FOUND, body = "")

          when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          an[Exception] mustBe thrownBy {
            connector.retrieveInboxPartial().futureValue
          }
        }
      }
    }

    "retrieveMessagePartial is called" which {
      "receives a 200 response" should {
        "return a populated InboxPartial" ignore {} //TODO: implement when connector talks to real secure-message-frontend service
      }

      "receives a non 200 response" should {
        "return a failed Future" ignore {} //TODO: implement when connector talks to real secure-message-frontend service
      }
    }
  }
}
