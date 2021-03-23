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
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach
import play.mvc.Http.Status.{BAD_GATEWAY, BAD_REQUEST, OK}
import testdata.CommonTestData
import uk.gov.hmrc.http._

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
  val clientId = "clientId"
  val conversationId = "conversationId"

  "SecureMessageFrontend" when {
    "retrieveInboxPartial is called" which {
      "receives a 200 response" should {
        "return a populated InboxPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          result mustBe InboxPartial(partialContent)
        }
      }

      "receives a non 200 response" should {
        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages)
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.failed(new BadGatewayException("Error")))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages)
          assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
        }
      }

      "is passed the user's eori number" should {
        "include the Enrolment tag as a query string parameter with the correct eori value" in {
          val httpResponse = HttpResponse(status = OK, body = "")

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
          verify(httpClient).GET[HttpResponse](anyString(), queryParamCaptor.capture())(any(), any(), any())
          val queryParamValue = queryParamCaptor.getValue().asInstanceOf[Seq[(String, String)]]

          queryParamValue.size mustBe 2
          queryParamValue(0) mustBe Tuple2("enrolment", s"HMRC-CUS-ORG~EoriNumber~${CommonTestData.eori}")
        }
      }

      "is passed the message filter tag of ExportMessages" should {
        "include the ExportMessages tag as a query string parameter" in {
          val httpResponse = HttpResponse(status = OK, body = "")

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
          verify(httpClient).GET[HttpResponse](anyString(), queryParamCaptor.capture())(any(), any(), any())
          val queryParamValue = queryParamCaptor.getValue().asInstanceOf[Seq[(String, String)]]

          queryParamValue.size mustBe 2
          queryParamValue(1) mustBe Tuple2("tag", "notificationType~CDS-EXPORTS")
        }
      }

      "is passed the message filter tag of ImportMessages" should {
        "include the ImportMessages tag as a query string parameter" in {
          val httpResponse = HttpResponse(status = OK, body = "")

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ImportMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
          verify(httpClient).GET[HttpResponse](anyString(), queryParamCaptor.capture())(any(), any(), any())
          val queryParamValue = queryParamCaptor.getValue().asInstanceOf[Seq[(String, String)]]

          queryParamValue.size mustBe 2
          queryParamValue(1) mustBe Tuple2("tag", "notificationType~CDS-IMPORTS")
        }
      }
    }

    "retrieveConversationPartial is called" which {
      "receives a 200 response" should {
        "return a populated InboxPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveConversationPartial(clientId, conversationId).futureValue

          result mustBe ConversationPartial(partialContent)
        }
      }

      "receives a non 200 response" should {
        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")

          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveConversationPartial(clientId, conversationId)
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
            .thenReturn(Future.failed(new BadGatewayException("Error")))

          val result = connector.retrieveConversationPartial(clientId, conversationId)
          assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
        }
      }
    }

    "calling retrieveConversationPartial" should {
      "include the 'showReplyForm' query string param" in {
        val httpResponse = HttpResponse(status = OK, "")

        when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        connector.retrieveConversationPartial(clientId, conversationId).futureValue

        val queryParamCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(httpClient).GET[HttpResponse](anyString(), queryParamCaptor.capture())(any(), any(), any())

        val queryParamValue = queryParamCaptor.getValue().asInstanceOf[Seq[(String, String)]]

        queryParamValue.size mustBe 1
        queryParamValue(0) mustBe Tuple2("showReplyForm", "true")
      }
    }

    "submitReply is called" which {
      "receives a 200 response" should {
        "return a None" in {
          val httpResponse = HttpResponse(status = OK, body = "")

          when(httpClient.POSTForm[HttpResponse](anyString(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value"))).futureValue

          result.isEmpty mustBe true
        }
      }

      "receives a 400 response" should {
        "return a Some ConversationPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = partialContent)

          when(httpClient.POSTForm[HttpResponse](anyString(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value"))).futureValue

          result.isDefined mustBe true
          result.get mustBe ConversationPartial(partialContent)
        }
      }

      "receives a response that is not a 200 or 400" should {
        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_GATEWAY, body = "")

          when(httpClient.POSTForm[HttpResponse](anyString(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value")))
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(httpClient.POSTForm[HttpResponse](anyString(), any(), any())(any(), any(), any()))
            .thenReturn(Future.failed(new BadGatewayException("Error")))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value")))
          assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
        }
      }
    }
  }

  "retrieveReplyResult is called" which {
    "receives a 200 response" should {
      "return a populated ReplyResultPartial" in {
        val partialContent = "<div>Some Content</div>"
        val httpResponse = HttpResponse(status = OK, body = partialContent)

        when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = connector.retrieveReplyResult(clientId, conversationId).futureValue

        result mustBe ReplyResultPartial(partialContent)
      }
    }

    "receives a non 200 response" should {
      "return a failed Future" in {
        val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")

        when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = connector.retrieveReplyResult(clientId, conversationId)
        assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
      }
    }

    "fails to connect to downstream service" should {
      "return a failed Future" in {
        when(httpClient.GET[HttpResponse](anyString(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadGatewayException("Error")))

        val result = connector.retrieveReplyResult(clientId, conversationId)
        assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
      }
    }
  }
}
