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

import com.google.inject.ImplementedBy
import config.{AppConfig, Crypto}
import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.PredefUtils._

import scala.concurrent.{ExecutionContext, Future}


class MongoCacheMapRepository @Inject()(
  mongo: ReactiveMongoApi,
  appConfig: AppConfig,
  crypto: Crypto
)(implicit ec: ExecutionContext) extends CacheMapRepository {

  import crypto._

  private val collectionName: String = "cache"
  private val expireAfterSeconds = "expireAfterSeconds"
  private val idField = "cacheId"
  private val dataField = "data"
  private val ttl = appConfig.mongodb.shortTtl

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  private val index = Index(
    key  = Seq(idField -> IndexType.Descending),
    name = Some(s"${idField}_index"),
    options = BSONDocument(expireAfterSeconds -> ttl.toSeconds)
  )

  val started: Future[Boolean] = collection.flatMap(_.indexesManager.ensure(index))
  
  def get(id: String): Future[Option[CacheMap]] =
    collection
      .flatMap(_.find(Json.obj(idField -> id), None).one[JsValue])
      .map(_.flatMap { json =>
        val id   = (json \ idField).asOpt[String]
        val data = (json \ dataField).asOpt[JsValue].flatMap(decrypt[Map[String, JsValue]])

        id.zip(data).map { case (a, b) => CacheMap(a, b) }
      })

  def put(cacheMap: CacheMap): Future[Unit] = {

    val selector = Json.obj(idField -> cacheMap.id)

    val modifier = Json.obj(
      "$set" -> Json.obj(
        dataField -> encrypt(cacheMap.data)
      )
    )

    collection.flatMap {
      _.findAndUpdate(selector, modifier, upsert = true).map(_ => ())
    }
  }
}

@ImplementedBy(classOf[MongoCacheMapRepository])
trait CacheMapRepository extends Repository {

  def get(id: String): Future[Option[CacheMap]]

  def put(cacheMap: CacheMap): Future[Unit]
}