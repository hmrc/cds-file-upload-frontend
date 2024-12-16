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

package services

import models.FileUploadAnswers
import play.api.Logging
import repositories.FileUploadAnswersRepository

import java.time.{ZoneOffset, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.Future

class FileUploadAnswersService @Inject() (val repository: FileUploadAnswersRepository) extends Logging {

  def findOne(eori: String, uuid: String): Future[Option[FileUploadAnswers]] = repository.findOne(eori, uuid)

  def findOneOrCreate(eori: String, uuid: String): Future[FileUploadAnswers] = repository.findOneOrCreate(eori, uuid)

  def findOneAndReplace(answers: FileUploadAnswers): Future[FileUploadAnswers] =
    repository.findOneAndReplace(answers.copy(updated = ZonedDateTime.now(ZoneOffset.UTC)))

  def remove(eori: String, uuid: String): Future[Unit] = repository.remove(eori, uuid)
}
