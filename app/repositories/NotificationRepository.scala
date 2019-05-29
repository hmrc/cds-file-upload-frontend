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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.Notification
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes._
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRepository @Inject()(mongo: ReactiveMongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext) extends ReactiveRepository[Notification, BSONObjectID](
  collectionName = "notifications",
  mongo = mongo.mongoConnector.db,
  domainFormat = Notification.notificationFormat,
  idFormat = ReactiveMongoFormats.objectIdFormats
) {

  override def indexes: Seq[Index] = Seq(
    Index(key = Seq(("fileReference", IndexType.Ascending)), name = Some("fileReferenceIndex")),
    Index(key = Seq(("createdAt", IndexType.Ascending)), name = Some("createdAtIndex"), options = BSONDocument("expireAfterSeconds" -> appConfig.notifications.ttlSeconds))
  )


  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = Future.successful(Seq.empty)
  
  def ensureIndex(index: Index)(implicit ec: ExecutionContext): Future[Unit] = {
    collection.indexesManager
      .create(index)
      .map(wr => Logger.warn(wr.toString))
      .recover {
        case t =>
          Logger.error(s"$message (${index.eventualName})", t)
      }
  }

  Future.sequence(indexes.map(ensureIndex))
}