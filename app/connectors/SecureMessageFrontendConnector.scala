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
import models.{ConversationPartial, InboxPartial}
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnector @Inject()(httpClient: HttpClient, config: AppConfig) extends Logging with Status {

  def retrieveInboxPartial()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[InboxPartial] =
    //TODO: reinstate 'Future.failed' and remove fakeInboxResponse once secure-message service is stable
    SecureMessageFrontendConnector.fakeInboxResponse

  /*httpClient
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
      }*/

  def retrieveConversationPartial(
    client: String,
    conversationId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ConversationPartial] =
    //TODO: reinstate httpClient call and remove fakeInboxResponse once secure-message service is stable
    SecureMessageFrontendConnector.fakeConversationResponse

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
}

object SecureMessageFrontendConnector {
  lazy val fakeInboxResponse: Future[InboxPartial] = Future.successful(InboxPartial(inboxPartial))

  lazy val fakeConversationResponse: Future[ConversationPartial] = Future.successful(ConversationPartial(conversationPartial))

  lazy val inboxPartial =
    """
    |    <h1 class="govuk-heading-l">Messages between you and customs authorities</h1>
    |
    |    <div>
    |      <div class="govuk-accordion" data-module="govuk-accordion" id="accordion-with-summary-sections" style="border-bottom: solid 1px #fff !important"><div class="govuk-accordion__controls"><button type="button" class="govuk-accordion__open-all" aria-expanded="true">Close all<span class="govuk-visually-hidden"> sections</span></button></div>
    |        <div class="govuk-accordion__section govuk-accordion__section--expanded">
    |          <div class="govuk-accordion__section-header">
    |            <h2 class="govuk-accordion__section-heading">
    |
    |            <button type="button" id="accordion-with-summary-sections-heading-1" aria-controls="accordion-with-summary-sections-content-1" class="govuk-accordion__section-button" aria-describedby="accordion-with-summary-sections-summary-1" aria-expanded="true">
    |                Your messages
    |              <span class="govuk-accordion__icon" aria-hidden="true"></span></button></h2>
    |            <div class="govuk-accordion__section-summary govuk-body" id="accordion-with-summary-sections-summary-1">
    |              Recent and unread messages
    |            </div>
    |          </div>
    |          <div id="accordion-with-summary-sections-content-1" class="govuk-accordion__section-content" aria-labelledby="accordion-with-summary-sections-heading-1" style="border-bottom: solid 1px #fff !important;">
    |
    |            <table class="govuk-table">
    |              <thead class="govuk-table__head">
    |                <tr class="govuk-table__row">
    |                  <th scope="col" class="govuk-table__header">Subject</th>
    |                  <th scope="col" class="govuk-table__header">Date</th>
    |                </tr>
    |              </thead>
    |              <tbody class="govuk-table__body">
    |               <tr class="govuk-table__row no-border" role="row">
    |                 <td role="cell" class="govuk-table__cell">
    |                   HMRC exports
    |                   <p class="govuk-body">
    |                     <a href="/cds-file-upload-service/conversation/cdcm/D-80542-20201122" class="govuk-link">MRN0GB00004112345678001111</a> needs action
    |                   </p>
    |                 </td>
    |                 <td role="cell" class="govuk-table__cell">
    |                   24th Feburary 2021
    |                 </td>
    |               </tr>
    |              </tbody>
    |            </table>
    |          </div>
    |        </div>
    |      </div>
    |    </div>""".stripMargin

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
     |      <h2 class="govuk-heading-m !-margin-bottom-9">Reply to this message</h2>
     |      <form class="form" action="single-message-answer" method="post" novalidate="">
     |        <div class="govuk-form-group">
     |          <label class="govuk-label" for="previous-message-reply">
     |            Your message (optional)
     |          </label>
     |
     |          <textarea class="govuk-textarea" id="previous-message-reply" name="previous-message-reply-1" rows="10" aria-describedby="previous-message-reply-hint"></textarea>
     |        </div>
     |        <input type="hidden" name="dec-previousMessageReplies" value="1">
     |        <input type="hidden" name="upload-mrn" value="20GB00004112345678001111">
     |
     |        <a href="/version39/messages/message-confirmation3" class="govuk-button">Reply</a>
     |      </form>
     |""".stripMargin
}
