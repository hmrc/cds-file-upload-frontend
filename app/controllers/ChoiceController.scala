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

import controllers.actions.{AuthAction, VerifiedEmailAction}
import forms.ChoiceForm
import forms.ChoiceForm.AllowedChoiceValues._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.FileUploadAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.choice_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ChoiceController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  answersService: FileUploadAnswersService,
  choicePage: choice_page,
  implicit val ec: ExecutionContext
) extends FrontendController(mcc) with I18nSupport {

  val actions = authenticate andThen verifiedEmail

  val onPageLoad: Action[AnyContent] = actions.async { implicit request =>
    answersService.remove(request.eori).map { _ =>
      Ok(choicePage(ChoiceForm.form))
    }
  }

  val onSubmit: Action[AnyContent] = actions { implicit request =>
    ChoiceForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(choicePage(formWithErrors)),
        validChoice =>
          validChoice.choice match {
            case SecureMessageInbox => Redirect(controllers.routes.InboxChoiceController.onPageLoad)
            case DocumentUpload     => Redirect(controllers.routes.MrnEntryController.onPageLoad())
            case _                  => throw new IllegalArgumentException
          }
      )
  }
}
