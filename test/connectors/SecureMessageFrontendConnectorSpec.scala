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

import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import play.mvc.Http.Status.{BAD_GATEWAY, BAD_REQUEST, OK}
import services.AuditService
import testdata.CommonTestData
import uk.gov.hmrc.http.{BadGatewayException, HttpResponse, UpstreamErrorResponse}

import java.net.URL
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SecureMessageFrontendConnectorSpec extends ConnectorSpec {

  private val mockAuditService = mock[AuditService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
  }

  val connector = new SecureMessageFrontendConnector(appConfig, httpClient, mockAuditService)(global)

  val clientId = "clientId"
  val conversationId = "conversationId"

  "SecureMessageFrontend" when {
    "retrieveInboxPartial is called" which {

      "receives a 200 response" should {

        "return a populated InboxPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          result mustBe InboxPartial(partialContent)
        }

        "audit the retrieval of the InboxPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          verify(mockAuditService).auditSecureMessageInbox(anyString(), anyString(), any[MessageFilterTag], anyString())(any())
        }
      }

      "receives a non 200 response" should {

        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages)
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }

        "not audit the attempted retrieval of the InboxPartial" in {
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages)

          verifyNoInteractions(mockAuditService)
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(execute[HttpResponse]).thenReturn(Future.failed(new BadGatewayException("Error")))

          val result = connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages)
          assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
        }
      }

      "is passed the message filter tag of ExportMessages" should {
        "have the correct size of queryParamValue array" in {
          val httpResponse = HttpResponse(status = OK, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ExportMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[URL])
          verify(httpClient).get(queryParamCaptor.capture())(any())
          val queryParamValue = queryParamCaptor.getValue.asInstanceOf[URL].getQuery.split('&')

          queryParamValue.length mustBe 2
          queryParamValue(0) mustBe s"enrolment=${AuthKey.enrolment}~EoriNumber~${CommonTestData.eori}"
          queryParamValue(1) mustBe "tag=notificationType~CDS-EXPORTS"
        }
      }

      "is passed the message filter tag of ImportMessages" should {
        "include the ImportMessages tag as a query string parameter" in {
          val httpResponse = HttpResponse(status = OK, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, ImportMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[URL])
          verify(httpClient).get(queryParamCaptor.capture())(any())
          val queryParamValue = queryParamCaptor.getValue.asInstanceOf[URL].getQuery.split('&')

          queryParamValue.length mustBe 2
          queryParamValue(0) mustBe s"enrolment=${AuthKey.enrolment}~EoriNumber~${CommonTestData.eori}"
          queryParamValue(1) mustBe "tag=notificationType~CDS-IMPORTS"
        }
      }

      "is passed the message filter tag of AllMessages" should {
        "not include the notificationType tag as a query string parameter" in {
          val httpResponse = HttpResponse(status = OK, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          connector.retrieveInboxPartial(CommonTestData.eori, AllMessages).futureValue

          val queryParamCaptor = ArgumentCaptor.forClass(classOf[URL])
          verify(httpClient).get(queryParamCaptor.capture())(any())
          val queryParamValue = queryParamCaptor.getValue.asInstanceOf[URL].getQuery.split('&')

          queryParamValue.length mustBe 1
          queryParamValue(0) mustBe s"enrolment=${AuthKey.enrolment}~EoriNumber~${CommonTestData.eori}"
        }
      }
    }

    "retrieveConversationPartial is called" which {

      "receives a 200 response" should {
        "return a populated InboxPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = OK, body = partialContent)
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveConversationPartial(clientId, conversationId).futureValue

          result mustBe ConversationPartial(partialContent)
        }
      }

      "receives a non 200 response" should {
        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.retrieveConversationPartial(clientId, conversationId)
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(execute[HttpResponse]).thenReturn(Future.failed(new BadGatewayException("Error")))

          val result = connector.retrieveConversationPartial(clientId, conversationId)
          assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
        }
      }
    }

    "calling retrieveConversationPartial" should {
      "include the 'showReplyForm' query string param" in {
        val httpResponse = HttpResponse(status = OK, "")
        when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

        connector.retrieveConversationPartial(clientId, conversationId).futureValue

        val queryParamCaptor = ArgumentCaptor.forClass(classOf[URL])
        verify(httpClient).get(queryParamCaptor.capture())(any())
        val queryParamValue = queryParamCaptor.getValue.asInstanceOf[URL].getQuery.split('&')

        queryParamValue.length mustBe 1
        queryParamValue(0) mustBe "showReplyForm=true"
      }
    }

    "submitReply is called" which {
      "receives a 200 response" should {
        "return a None" in {
          val httpResponse = HttpResponse(status = OK, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value"))).futureValue

          result.isEmpty mustBe true
        }
      }

      "receives a 400 response" should {
        "return a Some ConversationPartial" in {
          val partialContent = "<div>Some Content</div>"
          val httpResponse = HttpResponse(status = BAD_REQUEST, body = partialContent)
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value"))).futureValue

          result.isDefined mustBe true
          result.get mustBe ConversationPartial(partialContent)
        }
      }

      "receives a response that is not a 200 or 400" should {
        "return a failed Future" in {
          val httpResponse = HttpResponse(status = BAD_GATEWAY, body = "")
          when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

          val result = connector.submitReply(clientId, conversationId, Map("field" -> Seq("value")))
          assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          when(execute[HttpResponse]).thenReturn(Future.failed(new BadGatewayException("Error")))

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
        when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

        val result = connector.retrieveReplyResult(clientId, conversationId).futureValue

        result mustBe ReplyResultPartial(partialContent)
      }
    }

    "receives a non 200 response" should {
      "return a failed Future" in {
        val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")
        when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

        val result = connector.retrieveReplyResult(clientId, conversationId)
        assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
      }
    }

    "fails to connect to downstream service" should {
      "return a failed Future" in {
        when(execute[HttpResponse]).thenReturn(Future.failed(new BadGatewayException("Error")))

        val result = connector.retrieveReplyResult(clientId, conversationId)
        assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
      }
    }
  }
}
