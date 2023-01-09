/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.ProvidedBy
import config.AppConfig
import controllers.routes
import models.AuthKey
import models.UnauthorisedReason.UserIsAgent
import models.requests.{AuthenticatedRequest, SignedInUser}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Provider}
import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(
  val authConnector: AuthConnector,
  val config: Configuration,
  val env: Environment,
  eoriAllowList: EoriAllowList,
  mcc: MessagesControllerComponents
) extends AuthAction with AuthorisedFunctions with AuthRedirects {

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  override val parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

  private lazy val loginUrl = config.getOptional[String]("urls.login").getOrElse(throw new Exception("Missing login url configuration"))
  private lazy val continueLoginUrl =
    config.getOptional[String]("urls.loginContinue").getOrElse(throw new Exception("Missing continue login url configuration"))

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised((Individual or Organisation) and Enrolment(AuthKey.enrolment))
      .retrieve(allEnrolments and affinityGroup) {
        case allEnrolments ~ _ =>
          allEnrolments.getEnrolment(AuthKey.enrolment).flatMap(_.getIdentifier(AuthKey.identifierKey)) match {

            case Some(eori) if eoriAllowList.allows(eori.value) =>
              val signedInUser = SignedInUser(eori.value, allEnrolments)
              Future.successful(Right(AuthenticatedRequest(request, signedInUser)))

            case Some(_) => throw new UnauthorizedException("User is not authorized to use this service")
            case None    => throw InsufficientEnrolments()
          }
      }
      .recover {
        case _: UnsupportedAffinityGroup => Left(Results.Redirect(routes.UnauthorisedController.onAgentKickOut(UserIsAgent)))
        case _: NoActiveSession          => Left(Redirect(loginUrl, Map("continue" -> Seq(continueLoginUrl))))
        case _                           => Left(Redirect(routes.UnauthorisedController.onPageLoad))
      }

  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionRefiner[Request, AuthenticatedRequest]

@ProvidedBy(classOf[EoriAllowListProvider])
class EoriAllowList(val values: Seq[String]) {
  def allows(eori: String): Boolean = values.isEmpty || values.contains(eori)
}

class EoriAllowListProvider @Inject()(config: AppConfig) extends Provider[EoriAllowList] {
  override def get(): EoriAllowList =
    new EoriAllowList(config.allowList.eori)
}
