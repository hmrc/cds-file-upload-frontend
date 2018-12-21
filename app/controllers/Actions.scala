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

package controllers

import com.google.inject.{Inject, Singleton}
import domain.auth._
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Actions @Inject()(val authAction: AuthAction, val eoriAction: EORIAction) {

  def auth: ActionBuilder[AuthenticatedRequest] with ActionRefiner[Request, AuthenticatedRequest] =
    authAction

  def requireEori: ActionFilter[AuthenticatedRequest] =
    eoriAction
}

@Singleton
class AuthAction @Inject()(
  val authConnector: AuthConnector,
  val config: Configuration,
  val env: Environment
)(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest]
  with ActionRefiner[Request, AuthenticatedRequest]
  with AuthorisedFunctions
  with AuthRedirects {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(SignedInUser.authorisationPredicate)
      .retrieve(credentials and name and email and affinityGroup and internalId and allEnrolments) {
        case credentials ~ name ~ email ~ affinityGroup ~ internalId ~ enrolments =>
          val signedInUser = SignedInUser(credentials, name, email, affinityGroup, internalId, enrolments)
          Future.successful(Right(AuthenticatedRequest(request, signedInUser)))
      } recover {
        case _: NoActiveSession => Left(toGGLogin(request.uri))
        case _                  => Left(Redirect(routes.UnauthorisedController.onPageLoad))
      }
  }
}

@Singleton
class EORIAction extends ActionFilter[AuthenticatedRequest] {

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