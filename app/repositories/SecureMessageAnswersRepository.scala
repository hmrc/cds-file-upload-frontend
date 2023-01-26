/*
 * Copyright 2023 HM Revenue & Customs
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
import models.SecureMessageAnswers
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class SecureMessageAnswersRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SecureMessageAnswers](
      mongoComponent = mongoComponent,
      collectionName = "answers-secure-message",
      domainFormat = SecureMessageAnswers.format,
      indexes = SecureMessageAnswersRepository.indexes(appConfig)
    ) with RepositoryOps[SecureMessageAnswers] {

  override def classTag: ClassTag[SecureMessageAnswers] = implicitly[ClassTag[SecureMessageAnswers]]
  implicit val executionContext = ec

  def findOne(eori: String): Future[Option[SecureMessageAnswers]] = findOne("eori", eori)

  def findOneAndReplace(answers: SecureMessageAnswers): Future[SecureMessageAnswers] =
    findOneAndReplace("eori", answers.eori, answers)
}

object SecureMessageAnswersRepository {

  def indexes(appConfig: AppConfig): Seq[IndexModel] =
    List(
      IndexModel(ascending("eori"), IndexOptions().name("eoriIdx")),
      IndexModel(
        ascending("created"),
        IndexOptions()
          .name("ttl")
          .expireAfter(appConfig.secureMessageAnswersRepository.ttlSeconds, TimeUnit.SECONDS)
      )
    )
}
