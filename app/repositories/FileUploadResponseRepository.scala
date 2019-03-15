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

import com.google.inject.Inject
import config.{AppConfig, Crypto}
import models.FileUploadResponse
import models.requests.EORI
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

trait FileUploadResponseRepository extends Repository {

  def put(eori: EORI, data: List[FileUploadResponse]): Future[Unit]

  def getAll(eori: EORI): Future[List[FileUploadResponse]]

}

class MongoFileUploadResponseRepository @Inject()(mongo: ReactiveMongoApi,
                                                  appConfig: AppConfig,
                                                  crypto: Crypto)(implicit ex: ExecutionContext)
  extends FileUploadResponseRepository {

  import crypto._

  private val collectionName: String = "fileUpload"
  private val expireAfterSecond = "expireAfterSeconds"
  private val ttl = appConfig.mongodb.longTtl
  private val idField = "eori"
  private val dataField = "data"

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  private val index = Index(
    key = Seq(("eori", IndexType.Ascending)),
    name = Some("eori_index"),
    unique = true,
    options = BSONDocument(expireAfterSecond -> ttl.toSeconds)
  )

  def put(eori: EORI, data: List[FileUploadResponse]): Future[Unit] = {

    val selector = Json.obj(idField -> eori.value)

    val modifier = Json.obj(
      "$set" -> Json.obj(
        dataField -> encrypt(data)
      )
    )
    collection.flatMap {
      _.findAndUpdate(selector, modifier, upsert = true).map(_ => ())
    }
  }

  def getAll(eori: EORI): Future[List[FileUploadResponse]] =
    collection
      .flatMap(_.find(Json.obj(idField -> eori.value), None).one[JsValue])
      .map(_.flatMap { json =>
        (json \ dataField).asOpt[JsValue].flatMap(decrypt[List[FileUploadResponse]])
      }.getOrElse(Nil))

  val started: Future[Boolean] = collection.flatMap(_.indexesManager.ensure(index))

}


