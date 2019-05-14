/*
 * Copyright 2019 HM Revenue & Customs
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

package repositories

import javax.inject.{Inject, Singleton}
import models.Notification
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRepository @Inject()(mongo: ReactiveMongoComponent) extends ReactiveRepository[Notification, BSONObjectID]("notifications", mongo.mongoConnector.db, Notification.jsonFormat) {

//  def save(notification: Notification)(implicit ec: ExecutionContext): Future[Unit] =
//    collection.findAndUpdate(BSONDocument("_id" -> notification.fileReference), notification, upsert = true) map {
//      result => {
//        result.lastError.map(_.err.map(e => logger.error(s"Error saving the notification : $e")))
//        ()
//      }
//    }
}