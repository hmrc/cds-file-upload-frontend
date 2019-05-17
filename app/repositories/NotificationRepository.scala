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
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes._
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRepository @Inject()(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext) extends ReactiveRepository[Notification, BSONObjectID]("notifications", mongo.mongoConnector.db, Notification.jsonFormat) {
  override def indexes: Seq[Index] = {
    val ttlForUrl = 60

    Seq(Index(key  = Seq(("fileReference", IndexType.Ascending)), name = Some("fileReference")),
        Index(key     = Seq(("createdAt", IndexType.Ascending)), name = Some("createdAtIndex"), options = BSONDocument("expireAfterSeconds" -> ttlForUrl)))
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = Future.successful(Nil)

  def ensureIndex(index: Index)(implicit ec: ExecutionContext): Future[Unit] = {
    collection.indexesManager
      .create(index)
      .map(wr => Logger.warn(s"[GG-3616] ${wr.toString}"))
      .recover {
        case t =>
          Logger.error(s"[GG-3616] $message (${index.eventualName})", t)
      }
  }

  indexes.map(ensureIndex)

}