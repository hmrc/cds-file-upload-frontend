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

import com.google.inject.Inject
import config.AppConfig
import models._
import play.api.Logging
import play.api.http.Status
import play.api.libs.ws.writeableOf_urlEncodedForm
import services.AuditService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnector @Inject() (appConfig: AppConfig, httpClientV2: HttpClientV2, auditService: AuditService)(
  implicit ec: ExecutionContext
) extends Connector with Logging with Status {

  protected val httpClient: HttpClientV2 = httpClientV2

  def retrieveInboxPartial(eori: String, filter: MessageFilterTag)(implicit hc: HeaderCarrier): Future[InboxPartial] =
    fetchPartial(
      appConfig.microservice.services.secureMessaging.fetchInboxEndpoint,
      "the user's inbox",
      constructInboxEndpointQueryParams(AuthKey.enrolment, eori, filter)
    ).map { response =>
      auditService.auditSecureMessageInbox(AuthKey.enrolment, eori, filter, appConfig.microservice.services.secureMessaging.fetchInbox)
      InboxPartial(response.body)
    }

  def retrieveConversationPartial(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ConversationPartial] =
    fetchPartial(
      appConfig.microservice.services.secureMessaging.fetchMessageEndpoint(client, conversationId),
      s"the '$client/$conversationId' conversation",
      conversationEndpointQueryParams
    ).map(response => ConversationPartial(response.body))

  def submitReply(client: String, conversationId: String, reply: Map[String, Seq[String]])(
    implicit hc: HeaderCarrier
  ): Future[Option[ConversationPartial]] = {
    val submitEndpoint = appConfig.microservice.services.secureMessaging.submitReplyEndpoint(client, conversationId)
    post[Map[String, Seq[String]], HttpResponse](submitEndpoint, reply).flatMap { response =>
      response.status match {
        case OK          => Future.successful(None)
        case BAD_REQUEST => Future.successful(Some(ConversationPartial(response.body)))
        case statusCode =>
          Future.failed(UpstreamErrorResponse(s"Unhappy response($statusCode) posting reply form to secure-messaging-frontend", statusCode))
      }
    }.recoverWith { case exc: UpstreamErrorResponse =>
      logger.warn(
        s"Received a ${exc.statusCode} response from secure-messaging-frontend while submitting a reply for '$client/$conversationId'. ${exc.message}"
      )
      Future.failed(exc)
    }
  }

  def retrieveReplyResult(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ReplyResultPartial] =
    fetchPartial(
      appConfig.microservice.services.secureMessaging.replyResultEndpoint(client, conversationId),
      s"the success result of replying to '$client/$conversationId' conversation"
    ).map(response => ReplyResultPartial(response.body))

  private def fetchPartial(url: String, errorInfo: String, queryParams: Seq[(String, String)] = Seq.empty)(
    implicit hc: HeaderCarrier
  ): Future[HttpResponse] =
    get[HttpResponse](url, queryParams).flatMap { response =>
      response.status match {
        case OK => Future.successful(response)
        case statusCode =>
          Future.failed(UpstreamErrorResponse(s"Unhappy response($statusCode) fetching $errorInfo", statusCode))
      }
    }.recoverWith { case exc: UpstreamErrorResponse =>
      logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend while retrieving $errorInfo. ${exc.message}")
      Future.failed(exc)
    }

  private def constructInboxEndpointQueryParams(enrolment: String, eori: String, filter: MessageFilterTag): Seq[(String, String)] = {
    val enrolmentParameter = Seq(("enrolment", s"$enrolment~EoriNumber~$eori"))
    val filterParameter = if (filter.filterValue.isEmpty) Seq.empty else Seq(("tag", s"notificationType~${filter.filterValue}"))

    enrolmentParameter ++ filterParameter
  }

  private val conversationEndpointQueryParams: Seq[(String, String)] = Seq(("showReplyForm", "true"))
}
