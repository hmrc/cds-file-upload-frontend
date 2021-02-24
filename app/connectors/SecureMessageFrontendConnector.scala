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
import models.InboxPartial
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnector @Inject()(httpClient: HttpClient, config: AppConfig) extends Logging with Status {

  def retrieveConversationsPartial()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[InboxPartial] =
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
}

object SecureMessageFrontendConnector {
  lazy val fakeInboxResponse: Future[InboxPartial] = Future.successful(InboxPartial(inboxPartial))

  lazy val inboxPartial =
    """<div class="govuk-grid-column-full">
    |
    |
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
    |
    |
    |
    |<tr class="govuk-table__row no-border" role="row">
    |    <td role="cell" class="govuk-table__cell">
    |
    |            <!-- <span class="message-bubble active"></span>                         -->
    |            HMRC exports
    |            <p class="govuk-body">
    |              <a href="sfus-single-message.html?ucr=0GB00004112345678001111" class="govuk-link">MRN
    |                0GB00004112345678001111</a> needs action
    |            </p>
    |            <!-- <a class="no-link-colour" href="sfus-single-message.html?ucr=">Provide more information about MRN </a></span> -->
    |
    |    </td>
    |    <td role="cell" class="govuk-table__cell">
    |      24th Feburary 2021
    |    </td>
    |</tr>
    |
    |
    |
    |
    |              </tbody>
    |            </table>
    |          </div>
    |        </div>
    |
    |
    |
    |
    |        </div></div></div>""".stripMargin

}
