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

package connectors

import javax.inject.Inject
import models.UserAnswers
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import repositories.AnswersRepository

import scala.concurrent.{ExecutionContext, Future}

class AnswersConnector @Inject()(val repository: AnswersRepository)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def removeByEori(eori: String): Future[Unit] = repository.remove("eori" -> eori).map(_ => (): Unit)

  def findByEori(eori: String): Future[Option[UserAnswers]] = repository.find("eori" -> eori).map(_.headOption)

  def findOrCreate(eori: String): Future[UserAnswers] =
    findByEori(eori).flatMap {
      case Some(movementCache) => Future.successful(movementCache)
      case None                => save(UserAnswers(eori))
    }

  def upsert(answers: UserAnswers): Future[UserAnswers] = {
    val updated = answers.copy(updated = DateTime.now.withZone(DateTimeZone.UTC))
    repository
      .findAndUpdate(Json.obj("eori" -> updated.eori), Json.toJson(updated).as[JsObject])
      .map(_.value.map(_.as[UserAnswers]))
      .flatMap {
        case Some(cache) => Future.successful(cache)
        case None        => save(updated)
      }
  }

  private def save(answers: UserAnswers): Future[UserAnswers] = repository.insert(answers).map { res =>
    if (!res.ok) logger.error(s"Errors when persisting answers: ${res.writeErrors.mkString("--")}")
    answers
  }
}
