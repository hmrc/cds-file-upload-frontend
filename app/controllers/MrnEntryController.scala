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
import config.AppConfig
import controllers.actions._
import forms.MRNFormProvider
import models.requests.DataRequest
import models.{FileUploadAnswers, MRN}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.FileUploadAnswersService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.RefererUrlValidator
import views.html.{mrn_access_denied, mrn_entry}

import java.net.URLDecoder.decode
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
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc) with I18nSupport {

  private val form = formProvider()

  private def getBackLink(refererUrl: Option[String]): String = refererUrl.getOrElse(routes.ChoiceController.onPageLoad.url)

  def onPageLoad(maybeRefererUrl: Option[RedirectUrl] = None): Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async {
    implicit req =>
      val populatedForm = req.userAnswers.mrn.fold(form)(form.fill)

      val sanitisedRefererUrl = for {
        decodedUrl <- decodeRefererUrl(maybeRefererUrl)
        filteredUrl <- filterBadRefererUrl(decodedUrl)
      } yield filteredUrl

      sanitisedRefererUrl.map { backLink =>
        answersService
          .findOneAndReplace(req.userAnswers.copy(mrnPageRefererUrl = Some(backLink)))
          .map(_ => Ok(mrnEntry(populatedForm, getBackLink(Some(backLink)))))
      }.getOrElse(Future.successful(Ok(mrnEntry(populatedForm, getBackLink(req.userAnswers.mrnPageRefererUrl)))))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData).async { implicit req =>
    form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(mrnEntry(errorForm, getBackLink(req.userAnswers.mrnPageRefererUrl)))),
        updateUserAnswersAndRedirect(_, req.userAnswers)
      )
  }

  def autoFill(mrn: String, maybeRefererUrl: Option[RedirectUrl] = None): Action[AnyContent] =
    (authenticate andThen verifiedEmail andThen getData).async { implicit req =>
      val updatedAnswers = decodeRefererUrl(maybeRefererUrl)
        .map(decodedBackLink => req.userAnswers.copy(mrnPageRefererUrl = Some(decodedBackLink)))
        .getOrElse(req.userAnswers)

      MRN(mrn)
        .map(updateUserAnswersAndRedirect(_, updatedAnswers))
        .getOrElse(invalidMrnResponse(mrn))
    }

  private def updateUserAnswersAndRedirect(mrn: MRN, userAnswers: FileUploadAnswers): Future[Result] =
    answersService.findOneAndReplace(userAnswers.copy(mrn = Some(mrn))).map { _ =>
      Redirect(routes.ContactDetailsController.onPageLoad)
    }

  private def invalidMrnResponse(mrn: String)(implicit req: DataRequest[AnyContent]): Future[Result] =
    Future.successful(BadRequest(mrnAccessDenied(mrn)))

  private val decodeRefererUrl = (refererUrl: Option[RedirectUrl]) =>
    refererUrl.flatMap { url =>
      val maybeUrl = url.getEither(OnlyRelative) match {
        case Left(_)                => None
        case Right(safeRedirectUrl) => Some(safeRedirectUrl.url)
      }

      maybeUrl.map(decode(_, "UTF-8"))
    }

  private val filterBadRefererUrl = (refererUrl: String) => Some(refererUrl).filter(RefererUrlValidator.isValid)
}
