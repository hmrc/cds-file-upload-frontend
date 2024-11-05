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

package controllers

import controllers.actions.{AuthAction, MessageFilterAction, VerifiedEmailAction}
import forms.InboxChoiceForm
import models.requests.MessageFilterRequest
import models.{ExportMessages, MessageFilterTag, SecureMessageAnswers}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SecureMessageAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.messaging.inbox_choice

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InboxChoiceController @Inject() (
  mcc: MessagesControllerComponents,
  messageFilterAction: MessageFilterAction,
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  answersService: SecureMessageAnswersService,
  inboxChoice: inbox_choice,
  ec: ExecutionContext
) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val eContext: ExecutionContext = ec
  private val form = InboxChoiceForm.form

  val actions = authenticate andThen verifiedEmail andThen messageFilterAction

  val onExportsMessageChoice: Action[AnyContent] = actions.async { implicit request =>
    checkMessageFilterTag(ExportMessages.toString)
  }

  val onPageLoad: Action[AnyContent] = actions { implicit request =>
    val maybeCachedAnswer = InboxChoiceForm.messageFilterTagToChoice(request.secureMessageAnswers.filter)
    val populatedForm = maybeCachedAnswer.fold(form)(form.fill)

    Ok(inboxChoice(populatedForm))
  }

  val onSubmit: Action[AnyContent] = actions.async { implicit request =>
    InboxChoiceForm.form
      .bindFromRequest()
      .fold(formWithErrors => Future.successful(BadRequest(inboxChoice(formWithErrors))), form => checkMessageFilterTag(form.choice))
  }

  def checkMessageFilterTag(choice: String)(implicit request: MessageFilterRequest[_]): Future[Result] =
    MessageFilterTag.valueOf(choice) match {
      case None =>
        logger.error(s"InboxChoiceForm was sent an invalid MessageFilterTag value of '$choice'")
        Future.successful(BadRequest(inboxChoice(InboxChoiceForm.form)))

      case Some(tag) =>
        answersService.findOneAndReplace(SecureMessageAnswers(request.request.eori, tag)).map { _ =>
          Redirect(routes.SecureMessagingController.displayInbox)
        }
    }
}
