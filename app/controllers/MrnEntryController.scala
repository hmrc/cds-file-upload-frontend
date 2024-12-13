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

import com.google.inject.Singleton
import controllers.actions._
import forms.MRNFormProvider
import models.requests.DataRequest
import models.{FileUploadAnswers, MRN, SessionHelper}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.FileUploadAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{mrn_access_denied, mrn_entry}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MrnEntryController @Inject() (
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  verifiedEmail: VerifiedEmailAction,
  formProvider: MRNFormProvider,
  answersService: FileUploadAnswersService,
  mcc: MessagesControllerComponents,
  mrnEntry: mrn_entry,
  mrnAccessDenied: mrn_access_denied
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val form = formProvider()

  val onPageLoad: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async { implicit request =>
    val populatedForm = request.userAnswers.mrn.fold(form)(form.fill)
    Future.successful(Ok(mrnEntry(populatedForm)))
  }

  val onSubmit: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(errorForm => Future.successful(BadRequest(mrnEntry(errorForm))), updateUserAnswersAndRedirect(_, request.userAnswers))
  }

  def autoFill(mrn: String): Action[AnyContent] =
    (authenticate andThen verifiedEmail andThen getData).async { implicit request =>
      MRN(mrn)
        .map(updateUserAnswersAndRedirect(_, request.userAnswers))
        .getOrElse(invalidMrnResponse(mrn))
    }
  private def updateUserAnswersAndRedirect(mrn: MRN, userAnswers: FileUploadAnswers)(implicit request: DataRequest[AnyContent]): Future[Result] =
    answersService.findOneAndReplace(userAnswers.copy(mrn = Some(mrn))).map { _ =>
      Redirect(routes.ContactDetailsController.onPageLoad).addingToSession(SessionHelper.ANSWER_CACHE_ID -> userAnswers.uuid)
    }

  private def invalidMrnResponse(mrn: String)(implicit request: DataRequest[AnyContent]): Future[Result] =
    Future.successful(BadRequest(mrnAccessDenied(mrn)))
}
