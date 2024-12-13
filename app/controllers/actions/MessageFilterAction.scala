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

package controllers.actions

import models.{AllMessages, SecureMessageAnswers}
import models.requests.{MessageFilterRequest, VerifiedEmailRequest}
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.SecureMessageAnswersService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageFilterActionImpl @Inject() (val answersService: SecureMessageAnswersService, mcc: MessagesControllerComponents)
    extends MessageFilterAction {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  override protected def refine[A](request: VerifiedEmailRequest[A]): Future[Either[Result, MessageFilterRequest[A]]] =
    answersService.findOne(request.eori, "TESTING").map { maybeFilter =>
      maybeFilter match {
        case Some(filter) =>
          Right(MessageFilterRequest(request, filter))
        case None => Right(MessageFilterRequest(request, SecureMessageAnswers(request.eori, AllMessages)))
      }
    }
}

trait MessageFilterAction extends ActionRefiner[VerifiedEmailRequest, MessageFilterRequest]
