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

import com.google.inject.Inject
import config.AppConfig
import models.{ConversationPartial, InboxPartial, MessageFilterTag, ReplyResultPartial}
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) extends Logging with Status {

  def retrieveInboxPartial(eori: String, filter: MessageFilterTag)(implicit hc: HeaderCarrier): Future[InboxPartial] =
    fetchPartial(config.microservice.services.secureMessaging.fetchInboxEndpoint, "the user's inbox", constructInboxEndpointQueryParams(eori, filter))
      .map(response => InboxPartial(response.body))

  def retrieveConversationPartial(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ConversationPartial] =
    fetchPartial(
      config.microservice.services.secureMessaging.fetchMessageEndpoint(client, conversationId),
      s"the '$client/$conversationId' conversation"
    ).map(response => ConversationPartial(response.body))

  def retrieveReplyResult(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ReplyResultPartial] =
    SecureMessageFrontendConnector.fakeReplyResultPartial
  /*
    fetchPartial(
      config.microservice.services.secureMessaging.replyResultEndpoint(client, conversationId),
      s"the result of replying to '$client/$conversationId' conversation"
    )
    .map(response => ReplyResultPartial(response.body))
   */

  def submitReply(client: String, conversationId: String, reply: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Unit] =
    Future.successful(())
  /*
    httpClient
      .doPost(
        config.microservice.services.secureMessaging.submitReplyEndpoint(client, conversationId),
        Json.toJson(reply)
      )
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(())
          case statusCode =>
            Future.failed(UpstreamErrorResponse(s"Unhappy response submitting a reply for '$client/$conversationId' conversation", statusCode))
        }
      }
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend while submitting a reply for '$client/$conversationId'. ${exc.message}")
          Future.failed(exc)
      }
   */

  private def fetchPartial(url: String, errorInfo: String, queryParams: Seq[(String, String)] = Seq.empty)(
    implicit hc: HeaderCarrier
  ): Future[HttpResponse] =
    httpClient
      .GET[HttpResponse](url, queryParams)
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response)
          case statusCode =>
            Future.failed(UpstreamErrorResponse(s"Unhappy response fetching $errorInfo", statusCode))
        }
      }
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend while retrieving $errorInfo. ${exc.message}")
          Future.failed(exc)
      }

  private def constructInboxEndpointQueryParams(eori: String, filter: MessageFilterTag): Seq[(String, String)] = {
    val enrolmentParameter = ("enrolment", s"HMRC-CUS-ORG~EoriNumber~$eori")
    val filterParameter = ("tag", s"notificationType~${filter.filterValue}")

    Seq(enrolmentParameter, filterParameter)
  }
}

object SecureMessageFrontendConnector {
  lazy val fakeReplyResultPartial: Future[ReplyResultPartial] = Future.successful(ReplyResultPartial(replyResultPartial))

  lazy val replyResultPartial =
    s"""<div class="govuk-panel govuk-panel--confirmation">
       |  <h1 class="govuk-panel__title">Message sent</h1>
       |
       |  <div class="govuk-panel__body">
       |    We have received your message and will reply within 2 hours
       |  </div>
       |</div>
       |
       |<p class="govuk-body">We have also sent you an email confirmation.</p>
       |
       |<h2 class="govuk-heading-m">What happens next</h2>
       |<p class="govuk-body">You do not need to do anything now.</p>
       |<p class="govuk-body govuk-!-margin-bottom-6">We will contact you if we need more information.</p>
       |
       |<a href="/cds-file-upload-service/message-choice" class="govuk-button">Back to your messages</a>
       |""".stripMargin
}
