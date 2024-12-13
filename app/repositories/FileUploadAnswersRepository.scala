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

import com.mongodb.client.model.Indexes.ascending
import config.AppConfig
import models.FileUploadAnswers
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class FileUploadAnswersRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[FileUploadAnswers](
      mongoComponent = mongoComponent,
      collectionName = "answers",
      domainFormat = FileUploadAnswers.format,
      indexes = FileUploadAnswersRepository.indexes(appConfig)
    ) with RepositoryOps[FileUploadAnswers] {

  override def classTag: ClassTag[FileUploadAnswers] = implicitly[ClassTag[FileUploadAnswers]]
  implicit val executionContext: ExecutionContext = ec

  def findOne(eori: String, uuid: String): Future[Option[FileUploadAnswers]] =
    findOne("eori", eori, "uuid", uuid)

  def findOneOrCreate(eori: String, uuid: String): Future[FileUploadAnswers] =
    findOneOrCreate("eori", eori, "uuid", uuid, FileUploadAnswers(eori, uuid = uuid))

  def findOneAndReplace(answers: FileUploadAnswers): Future[FileUploadAnswers] =
    findOneAndReplace("eori", answers.eori, "uuid", answers.uuid, answers)

  def remove(eori: String, uuid: String): Future[Unit] = removeEvery("eori", eori, "uuid", uuid)
}

object FileUploadAnswersRepository {

  def indexes(appConfig: AppConfig): Seq[IndexModel] =
    List(
      IndexModel(ascending("eori"), IndexOptions().name("eoriIdx")),
      IndexModel(
        ascending("updated"),
        IndexOptions()
          .name("ttl")
          .expireAfter(appConfig.fileUploadAnswersRepository.ttlSeconds, TimeUnit.SECONDS)
      )
    )
}
