/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import javax.inject._
import models.Notification
import play.api.Logger
import repositories.NotificationRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class NotificationService @Inject()(repository: NotificationRepository) {

  private val logger = Logger(this.getClass)

  def save(notification: NodeSeq)(implicit ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    val fileReference = (notification \\ "FileReference").text
    val outcome = (notification \\ "Outcome").text
    val filename = (notification \\ "FileName").text
    logger.info("Notification payload: " + notification)
    if (fileReference.isEmpty || outcome.isEmpty) {
      logger.info("File reference: " + fileReference)
      logger.info("Outcome: " + outcome)
      Future.successful(Left(new BadRequestException("File reference or outcome not found in xml")))
    } else {
      repository
        .insert(Notification(fileReference, outcome, filename))
        .map(_ => Right(()))
        .recover { case e => Left(e) }
    }
  }
}
