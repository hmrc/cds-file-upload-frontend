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

package handlers

import config.ServiceUrls
import controllers.routes.UnauthorisedController
import models.UnauthorisedReason.UserIsAgent
import models.exceptions.InvalidFeatureStateException
import play.api.Logging
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{InternalServerError, NotFound, Redirect}
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, NoActiveSession, UnsupportedAffinityGroup}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.error_template

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (override val messagesApi: MessagesApi, errorTemplate: error_template)(
  implicit executionContext: ExecutionContext,
  serviceUrls: ServiceUrls
) extends FrontendErrorHandler with Logging {

  implicit val ec: ExecutionContext = executionContext

  override def standardErrorTemplate(titleKey: String, heading: String, message: String)(implicit requestHeader: RequestHeader): Future[Html] = {
    implicit val request: Request[_] = Request(requestHeader, "")
    Future.successful(defaultErrorTemplate(titleKey, heading, message))
  }

  override def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] = {
    val result = ex match {
      case _: NoActiveSession              => Redirect(serviceUrls.login, Map("continue" -> Seq(serviceUrls.loginContinue)))
      case _: InsufficientEnrolments       => Redirect(UnauthorisedController.onPageLoad.url)
      case _: UnsupportedAffinityGroup     => Redirect(UnauthorisedController.onAgentKickOut(UserIsAgent))
      case _: InvalidFeatureStateException => notFound(Request(rh, ""))
      case _                               => internalServerError(Request(rh, ""))
    }
    Future.successful(result)
  }

  def defaultErrorTemplate(titleKey: String = "global.error.title", heading: String = "", message: String = "")(
    implicit request: Request[_]
  ): Html = {
    lazy val messages = messagesApi.preferred(request).messages
    val headingText = if (heading.isEmpty) messages("global.error.heading") else heading
    val messageText = if (message.isEmpty) messages("global.error.message") else message
    errorTemplate(titleKey, headingText, messageText)
  }

  def internalServerError(implicit request: Request[_]): Result =
    InternalServerError(defaultErrorTemplate()).withHeaders(CACHE_CONTROL -> "no-cache")

  def notFound(implicit request: Request[_]): Result = {
    lazy val messages = messagesApi.preferred(request).messages
    val heading = messages("global.error.pageNotFound.heading")
    val message = messages("global.error.pageNotFound.message")
    NotFound(defaultErrorTemplate("global.error.pageNotFound.title", heading, message)).withHeaders(CACHE_CONTROL -> "no-cache")
  }
}
