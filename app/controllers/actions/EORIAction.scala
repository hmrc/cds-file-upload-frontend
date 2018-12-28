/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.routes
import models.requests.{AuthenticatedRequest, SignedInUser}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.Future

class EORIActionImpl extends EORIAction {

  implicit class OptionOps[A](val optionA: Option[A]) {

    def swap[B](b: B): Option[B] = optionA match {
      case Some(_) => None
      case None    => Some(b)
    }
  }

  override protected def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] =
    Future.successful(
      request.user.enrolments
        .getEnrolment(SignedInUser.cdsEnrolmentName)
        .flatMap(_.getIdentifier(SignedInUser.eoriIdentifierKey))
        .filter(_.value.nonEmpty)
        .swap(Redirect(routes.UnauthorisedController.onPageLoad)))
}

trait EORIAction extends ActionFilter[AuthenticatedRequest]
