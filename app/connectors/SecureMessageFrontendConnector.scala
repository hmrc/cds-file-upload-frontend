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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageFrontendConnector @Inject()(httpClient: HttpClient, config: AppConfig) extends Logging with Status {

  def retrieveConversationsPartial()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[InboxPartial] =
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
}
