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

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.Inject
import config.AppConfig
import models.{ConversationPartial, InboxPartial, ReplyResultPartial}
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

class SecureMessageFrontendConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) extends Logging with Status {

  def retrieveInboxPartial()(implicit hc: HeaderCarrier): Future[InboxPartial] =
    httpClient
      .GET[HttpResponse](config.microservice.services.secureMessaging.fetchInboxEndpoint)
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(InboxPartial(response.body))
          case statusCode =>
            Future.failed(UpstreamErrorResponse("Unhappy response fetching user inbox!", statusCode))
        }
      }
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(
            s"Received a ${exc.statusCode} response from secure-messaging-frontend service while retrieving the user's inbox. ${exc.message}"
          )

          Future.failed(exc)
      }

  def retrieveConversationPartial(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ConversationPartial] =
    //TODO: reinstate httpClient call and remove fakeInboxResponse once secure-message service is stable
    SecureMessageFrontendConnector.fakeConversationPartial

  /*httpClient
      .GET[HttpResponse](config.microservice.services.secureMessaging.fetchMessageEndpoint(client, conversationId))
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(ConversationPartial(response.body))
          case statusCode =>
            Future.failed(UpstreamErrorResponse("Unhappy response fetching a conversation!", statusCode))
        }
      }
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend service while retrieving a user's conversation with params '$client/$conversationId' . ${exc.message}")
          Future.failed(exc)
      }*/

  def retrieveReplyResult(client: String, conversationId: String)(implicit hc: HeaderCarrier): Future[ReplyResultPartial] =
    SecureMessageFrontendConnector.fakeReplyResultPartial
  /*httpClient
      .GET[HttpResponse](config.microservice.services.secureMessaging.replyResultEndpoint(client, conversationId))
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(ReplyPartial(response.body))
          case statusCode =>
            Future.failed(UpstreamErrorResponse("Unhappy response retrieving the Reply result a conversation!", statusCode))
        }
      }
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend service while retrieving the result of '$client/$conversationId''s reply . ${exc.message}")
          Future.failed(exc)
      }*/

  def submitReply(client: String, conversationId: String, reply: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Unit] =
    Future.successful(())

  /*
    httpClient
      .doPost(
        config.microservice.services.secureMessaging.submitReplyEndpoint(client, conversationId),
        Json.toJson(reply)
      )
      .map(_ => ())
      .recoverWith {
        case exc: UpstreamErrorResponse =>
          logger.warn(s"Received a ${exc.statusCode} response from secure-messaging-frontend service while submitting a message reply for '$client/$conversationId'. ${exc.message}")
          Future.failed(exc)
      }
 */

}

object SecureMessageFrontendConnector {
  lazy val fakeConversationPartial: Future[ConversationPartial] = Future.successful(ConversationPartial(conversationPartial))

  lazy val fakeReplyResultPartial: Future[ReplyResultPartial] = Future.successful(ReplyResultPartial(replyResultPartial))

  lazy val conversationPartial =
    """<a href="/cds-file-upload-service/messages" class="govuk-back-link">Back</a> 
     |
     |      <h1 class="govuk-heading-m govuk-!-margin-bottom-2">Provide more information about MRN 20GB00004112345678001111 - Case 676767</h1>
     |      <span class="govuk-caption-m govuk-!-margin-bottom-5"><strong>HMRC sent</strong> this message on 10 November 2020 at 8:23am</span>
     |
     |      <p class="govuk-body"><span class="govuk-caption-m govuk-!-font-size-16"><strong>EORI:</strong> GB6132244152.</span></p>
     |
     |      <p class="govuk-body">
     |        Dear Trader,
     |      </p>
     |
     |      <p class="govuk-body">
     |        Your CITES Import or Export permit. Commission Reg 160/2017 on the protection of species of wild fauna and flora is no longer valid. Please send a valid permit by following these steps:
     |      </p>
     |
     |
     |      <ul class="govuk-list govuk-list--bullet">
     |        <li>Use the link on this page to upload the document</li>
     |        <li>Attach it to your declaration (you may need to re-enter the correct MRN shown on this message)</li>
     |        <li>Return to this message and reply to let us know it is ready for us to check.
     |          If it is in order we will approve your consignment.
     |        </li>
     |      </ul>
     |
     |      <p class="govuk-body">National Clearance Hub</p>
     |
     |      <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
     |
     |      <div class="form-group">
     |        <h2 class="govuk-heading-m !-margin-bottom-9">Reply to this message</h2>
     |        <form class="form" action="/cds-file-upload-service/conversation/cdcm/D-80542-20201122" method="POST">
     |          <input type="hidden" name="csrfToken" value="[CSRF_TOKEN_TO_REPLACE]"/>
     |          <div class="govuk-form-group">
     |            <label class="govuk-label" for="messageReply">Your message (optional)</label>
     |
     |            <textarea class="govuk-textarea" id="previous-message-reply" name="previous-message-reply-1" rows="10" aria-describedby="previous-message-reply-hint"></textarea>
     |          </div>
     |          <input type="hidden" name="dec-previousMessageReplies" value="1">
     |          <input type="hidden" name="upload-mrn" value="20GB00004112345678001111">
     |
     |          <button id="submit" class="button">Reply</button>
     |        </form>
     |      </div>
     |""".stripMargin

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
       |<a href="declaration-messages" class="govuk-button">Back to your messages</a>
       |""".stripMargin
}
