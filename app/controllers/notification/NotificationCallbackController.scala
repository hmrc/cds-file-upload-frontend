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

package controllers.notification

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import services.NotificationService
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class NotificationCallbackController @Inject()(notificationService: NotificationService, val appConfig: AppConfig, mcc: MessagesControllerComponents)(
  implicit ec: ExecutionContext
) extends FrontendController(mcc) {

  def onNotify = Action.async(parse.xml) { implicit req =>
    val authHeader = req.headers.toSimpleMap.get("Authorization")
    val authToken = appConfig.notifications.authToken

    authHeader match {
      case Some(ah) if ah == authToken =>
        saveNotification(req.body)
      case _ =>
        Logger.warn(s"Failed to auth: $authHeader")
        Future.successful(Unauthorized)
    }
  }

  private def saveNotification(notification: NodeSeq) =
    notificationService.save(notification) map {
      case Right(_) => Accepted
      case Left(e: BadRequestException) =>
        Logger.error(s"Failed to save invalid notification: $notification", e)
        BadRequest
      case Left(e) =>
        Logger.error(s"Failed to save notification: $notification", e)
        InternalServerError
    }
}
