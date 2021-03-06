/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import models.FileUploadAnswers
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import repositories.FileUploadAnswersRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadAnswersService @Inject()(val repository: FileUploadAnswersRepository)(implicit ec: ExecutionContext) extends Logging {

  def removeByEori(eori: String): Future[Unit] = repository.remove("eori" -> eori).map(_ => (): Unit)

  def findByEori(eori: String): Future[Option[FileUploadAnswers]] = repository.find("eori" -> eori).map(_.headOption)

  def findOrCreate(eori: String): Future[FileUploadAnswers] =
    findByEori(eori).flatMap {
      case Some(movementCache) => Future.successful(movementCache)
      case None                => save(FileUploadAnswers(eori))
    }

  def upsert(answers: FileUploadAnswers): Future[Option[FileUploadAnswers]] = {
    val updated = answers.copy(updated = DateTime.now.withZone(DateTimeZone.UTC))
    repository
      .findAndUpdate(Json.obj("eori" -> updated.eori), Json.toJson(updated).as[JsObject], upsert = true)
      .map(_.value.map(_.as[FileUploadAnswers]))
  }

  private def save(answers: FileUploadAnswers): Future[FileUploadAnswers] = repository.insert(answers).map { res =>
    if (!res.ok) logger.error(s"Errors when persisting file upload answers: ${res.writeErrors.mkString("--")}")
    answers
  }
}
