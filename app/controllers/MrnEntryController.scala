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

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.Singleton
import config.SecureMessagingConfig
import controllers.actions._
import forms.MRNFormProvider
import models.{EORI, MRN}
import models.requests.DataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{FileUploadAnswersService, MrnDisValidator}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{mrn_access_denied, mrn_entry}

@Singleton
class MrnEntryController @Inject()(
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  verifiedEmail: VerifiedEmailAction,
  formProvider: MRNFormProvider,
  answersService: FileUploadAnswersService,
  mcc: MessagesControllerComponents,
  mrnEntry: mrn_entry,
  mrnDisValidator: MrnDisValidator,
  mrnAccessDenied: mrn_access_denied,
  secureMessagingConfig: SecureMessagingConfig
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val form = formProvider()

  lazy val backLinkUrl: String =
    if (secureMessagingConfig.isSecureMessagingEnabled) routes.ChoiceController.onPageLoad.url
    else routes.StartController.displayStartPage.url

  def onPageLoad: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData) { implicit req =>
    val populatedForm = req.userAnswers.mrn.fold(form)(form.fill)
    Ok(mrnEntry(populatedForm, backLinkUrl))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async { implicit req =>
    form
      .bindFromRequest()
      .fold(errorForm => Future.successful(BadRequest(mrnEntry(errorForm, backLinkUrl))), mrn => checkMrnExistenceAndOwnership(mrn))
  }

  def autoFill(mrn: String): Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async { implicit req =>
    MRN(mrn)
      .map(checkMrnExistenceAndOwnership(_))
      .getOrElse(invalidMrnResponse(mrn))
  }

  private def checkMrnExistenceAndOwnership(mrn: MRN)(implicit hc: HeaderCarrier, req: DataRequest[AnyContent]): Future[Result] =
    mrnDisValidator.validate(mrn, EORI(req.request.eori)).flatMap {
      case false => invalidMrnResponse(mrn.value)
      case true =>
        answersService.upsert(req.userAnswers.copy(mrn = Some(mrn))).map { _ =>
          Redirect(routes.ContactDetailsController.onPageLoad())
        }
    }

  private def invalidMrnResponse(mrn: String)(implicit req: DataRequest[AnyContent]): Future[Result] =
    Future.successful(BadRequest(mrnAccessDenied(mrn)))
}
