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

import javax.inject.Inject
import controllers.routes
import models.requests.{AuthenticatedRequest, SignedInUser}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector, val config: Configuration, val env: Environment, mcc: MessagesControllerComponents)
    extends AuthAction with AuthorisedFunctions with AuthRedirects {

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  override val parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

  lazy val loginUrl = config.getOptional[String]("urls.login").getOrElse(throw new Exception("Missing login url configuration"))
  lazy val continueLoginUrl =
    config.getOptional[String]("urls.loginContinue").getOrElse(throw new Exception("Missing continue login url configuration"))

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(SignedInUser.authorisationPredicate)
      .retrieve(
        credentials and
          name and
          email and
          affinityGroup and
          internalId and
          allEnrolments
      ) {

        case Some(credentials) ~ Some(name) ~ email ~ affinityGroup ~ Some(identifier) ~ enrolments =>
          val signedInUser = SignedInUser(credentials, name, email, affinityGroup, identifier, enrolments)
          Future.successful(Right(AuthenticatedRequest(request, signedInUser)))

        case _ =>
          //TODO Change this msg
          throw new UnauthorizedException("Unable to retrieve internal Id")

      } recover {
      case _: NoActiveSession => Left(Redirect(loginUrl, Map("continue" -> Seq(continueLoginUrl))))
      case _                  => Left(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionRefiner[Request, AuthenticatedRequest]
