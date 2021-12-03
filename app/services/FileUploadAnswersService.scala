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
import play.api.Logging
import repositories.FileUploadAnswersRepository

import java.time.{ZoneOffset, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadAnswersService @Inject()(val repository: FileUploadAnswersRepository)(implicit ec: ExecutionContext) extends Logging {

  def findOne(eori: String): Future[Option[FileUploadAnswers]] = repository.findOne("eori", eori)

  def findOneAndRemove(eori: String): Future[Option[FileUploadAnswers]] = repository.findOneAndRemove("eori", eori)

  def findOneOrCreate(eori: String): Future[FileUploadAnswers] =
    repository.findOneOrCreate("eori", eori, FileUploadAnswers(eori)) map {
      case Some(answers) => answers
      case None =>
        logger.warn(s"Error when persisting file upload answers for eori($eori)")
        FileUploadAnswers(eori)
    }

  def findOneAndReplace(answers: FileUploadAnswers): Future[Option[FileUploadAnswers]] = {
    val updated = answers.copy(updated = ZonedDateTime.now(ZoneOffset.UTC))
    repository.findOneAndReplace("eori", answers.eori, updated) map { result =>
      if (result.isEmpty) logger.warn(s"Error when upserting $updated")
      result
    }
  }

  def remove(eori: String): Future[Unit] = repository.removeEvery("eori", eori)
}
