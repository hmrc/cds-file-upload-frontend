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

package services

import javax.inject._
import models.Notification
import reactivemongo.api.commands.WriteResult
import repositories.NotificationRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

@Singleton
class NotificationService @Inject()(repository: NotificationRepository) {

  def save(notification: Elem)(implicit ec: ExecutionContext): Future[WriteResult] = {
    val fileReference = (notification \\ "FileReference").text
    val outcome = (notification \\ "Outcome").text

    println("&" * 100)
    println("saving notification to cache for file ref: " + fileReference)
    println("&" * 100)
    repository.insert(Notification(fileReference, outcome))
  }

  def drop(implicit ec: ExecutionContext): Future[Boolean] = repository.drop
}
