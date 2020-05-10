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

package controllers.actions

import com.google.inject.Inject
import connectors.AnswersConnector
import models.UserAnswers
import models.requests.{EORIRequest, OptionalDataRequest}
import play.api.mvc.{ActionTransformer, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject()(val answersConnector: AnswersConnector, mcc: MessagesControllerComponents) extends DataRetrievalAction {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  override protected def transform[A](request: EORIRequest[A]): Future[OptionalDataRequest[A]] = {
    val id = request.user.internalId
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val answersFuture = answersConnector.findOrCreate(request.eori)
    for {
      answers <- answersFuture
    } yield OptionalDataRequest(request, answers)
  }
}

trait DataRetrievalAction extends ActionTransformer[EORIRequest, OptionalDataRequest]
