/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.actions.{AuthAction, VerifiedEmailAction}
import forms.InboxChoiceForm
import models.{ExportMessages, MessageFilterTag, SecureMessageAnswers}
import models.requests.VerifiedEmailRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.Logging
import services.SecureMessageAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.messaging.inbox_choice

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InboxChoiceController @Inject()(
  mcc: MessagesControllerComponents,
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  answersService: SecureMessageAnswersService,
  inboxChoice: inbox_choice,
  ec: ExecutionContext
) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val eContext = ec

  val actions = authenticate andThen verifiedEmail

  val onExportsMessageChoice: Action[AnyContent] = actions.async { implicit request =>
    checkMessageFilterTag(ExportMessages.toString)
  }

  val onPageLoad: Action[AnyContent] = actions { implicit request =>
    Ok(inboxChoice(InboxChoiceForm.form))
  }

  val onSubmit: Action[AnyContent] = actions.async { implicit request =>
    InboxChoiceForm.form
      .bindFromRequest()
      .fold(formWithErrors => Future.successful(BadRequest(inboxChoice(formWithErrors))), { form =>
        checkMessageFilterTag(form.choice)
      })
  }

  private def checkMessageFilterTag(choice: String)(implicit req: VerifiedEmailRequest[AnyContent]): Future[Result] =
    MessageFilterTag.valueOf(choice) match {
      case None =>
        logger.error(s"InboxChoiceForm was sent an invalid MessageFilterTag value of '$choice'")
        Future.successful(BadRequest(inboxChoice(InboxChoiceForm.form)))

      case Some(tag) =>
        answersService.findOneAndReplace(SecureMessageAnswers(req.eori, tag)).map { _ =>
          Redirect(routes.SecureMessagingController.displayInbox)
        }
    }
}
