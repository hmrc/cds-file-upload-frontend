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

import config.{AppConfig, CDSFileUpload}
import javax.inject.Inject
import models.Notification
import play.api.Logger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class CdsFileUploadConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  private val cdsFileUploadConfig: CDSFileUpload = appConfig.microservice.services.cdsFileUpload

  def getNotification(reference: String)(implicit hc: HeaderCarrier): Future[Option[Notification]] =
    httpClient
      .GET[Option[Notification]](cdsFileUploadConfig.fetchNotificationEndpoint(reference))
      .map { notificationOpt =>
        notificationOpt match {
          case Some(notification) =>
            logger.info(s"Fetched notification: $notification")
          case None =>
            logger.info(s"There is no notification with reference: $reference")
        }
        notificationOpt
      }
}
