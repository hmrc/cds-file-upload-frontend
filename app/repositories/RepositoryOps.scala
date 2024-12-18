/*
 * Copyright 2024 HM Revenue & Customs
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

import com.mongodb.ErrorCategory.DUPLICATE_KEY
import com.mongodb.client.model.{ReturnDocument, Updates}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import org.mongodb.scala.{Document, MongoCollection, MongoWriteException}
import play.api.Logging
import play.api.libs.json.{Json, Writes}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait RepositoryOps[T] extends Logging {

  implicit def classTag: ClassTag[T]
  implicit val executionContext: ExecutionContext

  val collection: MongoCollection[T]

  def findAll: Future[Seq[T]] =
    collection.find().toFuture()

  def findAll[V](keyId: String, keyValue: V): Future[Seq[T]] =
    collection.find(equal(keyId, keyValue)).toFuture()

  def findOne[V](keyId: String, keyValue: V, keyId2: String, keyValue2: V): Future[Option[T]] =
    collection
      .find(and(equal(keyId, keyValue), equal(keyId2, keyValue2)))
      .toFuture()
      .map(_.headOption)

  /*
   Find one and return if a document with keyId=keyValue exists,
   or create "document: T" if a document with keyId=keyValue does NOT exists.
   */
  def findOneOrCreate[V](keyId1: String, keyValue1: V, keyId2: String, keyValue2: V, document: => T)(implicit writes: Writes[T]): Future[T] =
    collection
      .findOneAndUpdate(
        filter = and(equal(keyId1, keyValue1), equal(keyId2, keyValue2)),
        update = Updates.setOnInsert(BsonDocument(Json.toJson(document).toString)),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()

  /*
   Find one and replace with "document: T" if a document with keyId=keyValue exists,
   or create "document: T" if a document with keyId=keyValue does NOT exists.
   */
  def findOneAndReplace[V](keyId1: String, keyValue1: V, keyId2: String, keyValue2: V, document: T): Future[T] =
    collection
      .findOneAndReplace(
        filter = and(equal(keyId1, keyValue1), equal(keyId2, keyValue2)),
        replacement = document,
        options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()

  def findOneAndRemove[V](keyId: String, keyValue: V): Future[Option[T]] =
    collection.findOneAndDelete(equal(keyId, keyValue)).toFutureOption()

  def indexList: Future[Seq[Document]] = collection.listIndexes().toFuture()

  def insertOne(document: T): Future[Either[WriteError, T]] =
    collection
      .insertOne(document)
      .toFuture()
      .map(_ => Right(document))
      .recover {
        case exc: MongoWriteException if exc.getError.getCategory == DUPLICATE_KEY =>
          Left(DuplicateKey(exc.getError.getMessage))
      }

  def removeAll: Future[Unit] =
    collection.deleteMany(BsonDocument()).toFuture().map(_ => ())

  def removeEvery[V](keyId: String, keyValue: V, keyId2: String, keyValue2: V): Future[Unit] =
    collection.deleteMany(and(equal(keyId, keyValue), equal(keyId2, keyValue2))).toFuture().map(_ => ())

  def removeOne[V](keyId: String, keyValue: V): Future[Unit] =
    collection.deleteOne(equal(keyId, keyValue)).toFuture().map(_ => ())

  def size: Future[Long] = collection.countDocuments().toFuture()
}

sealed abstract class WriteError(message: String)

case class DuplicateKey(message: String) extends WriteError(message)
