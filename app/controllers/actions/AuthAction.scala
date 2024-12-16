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

import config.ServiceUrls
import controllers.routes.UnauthorisedController
import models.AuthKey
import models.UnauthorisedReason.UserIsAgent
import models.requests.{AuthenticatedRequest, SignedInUser}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject() (val authConnector: AuthConnector, mcc: MessagesControllerComponents, serviceUrls: ServiceUrls)
    extends AuthAction with AuthorisedFunctions with Logging {

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  override val parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised((Individual or Organisation) and Enrolment(AuthKey.enrolment))
      .retrieve(allEnrolments and affinityGroup) { case allEnrolments ~ _ =>
        allEnrolments.getEnrolment(AuthKey.enrolment).flatMap(_.getIdentifier(AuthKey.identifierKey)) match {
          case Some(eori) =>
            val signedInUser = SignedInUser(eori.value, allEnrolments)
            Future.successful(Right(AuthenticatedRequest(request, signedInUser)))

          case _ => throw InsufficientEnrolments("User has insufficient enrolments")
        }
      }
      .recover {
        case exc: NoActiveSession =>
          unauthorized(exc, Redirect(serviceUrls.login, Map("continue" -> Seq(serviceUrls.loginContinue))), request)

        case exc: UnsupportedAffinityGroup =>
          unauthorized(exc, Redirect(UnauthorisedController.onAgentKickOut(UserIsAgent)), request)

        case exc => unauthorized(exc, Redirect(UnauthorisedController.onPageLoad), request)
      }
  }

  private def unauthorized[A](throwable: Throwable, result: Result, request: Request[A]): Either[Result, AuthenticatedRequest[A]] = {
    val referer = s"Raw-Request-URI='${request.headers.get("Raw-Request-URI").getOrElse("unknown")}'"

    logger.info(s"User rejected with ${throwable.getMessage}. $referer")
    Left(result)
  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionRefiner[Request, AuthenticatedRequest]
