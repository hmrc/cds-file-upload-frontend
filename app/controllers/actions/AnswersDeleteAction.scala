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

import connectors.AnswersConnector
import javax.inject.{Inject, Singleton}
import models.requests.EORIRequest
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnswersDeleteActionImpl @Inject()(val answersConnector: AnswersConnector)(implicit val exc: ExecutionContext) extends AnswersDeleteAction {
  override def filter[A](request: EORIRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    answersConnector.removeByEori(request.eori).map(_ => None)
  }

  override protected def executionContext: ExecutionContext = exc
}

trait AnswersDeleteAction extends ActionFilter[EORIRequest]