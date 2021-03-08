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

import models.SecureMessageAnswers
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import repositories.SecureMessageAnswersRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecureMessageAnswersService @Inject()(val repository: SecureMessageAnswersRepository)(implicit ec: ExecutionContext) extends Logging {

  def findByEori(eori: String): Future[Option[SecureMessageAnswers]] = repository.find("eori" -> eori).map(_.headOption)

  def upsert(answers: SecureMessageAnswers): Future[Option[SecureMessageAnswers]] = {
    val updated = answers.copy(created = DateTime.now.withZone(DateTimeZone.UTC))
    repository
      .findAndUpdate(Json.obj("eori" -> updated.eori), Json.toJson(updated).as[JsObject], upsert = true)
      .map(_.value.map(_.as[SecureMessageAnswers]))
  }
}
