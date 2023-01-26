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

package controllers

import com.google.inject.Singleton
import connectors.CdsFileUploadConnector
import controllers.actions._
import metrics.MetricIdentifiers._
import metrics.SfusMetrics
import models.requests.VerifiedEmailRequest
import models.{FileUpload, MRN}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.FileUploadAnswersService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.upload_your_files_confirmation

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadYourFilesReceiptController @Inject() (
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  cdsFileUploadConnector: CdsFileUploadConnector,
  metrics: SfusMetrics,
  uploadYourFilesConfirmation: upload_your_files_confirmation,
  answersService: FileUploadAnswersService
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val onPageLoad: Action[AnyContent] = (authenticate andThen verifiedEmail).async { implicit req =>
    answersService.findOneAndRemove(req.eori).flatMap { maybeUserAnswers =>
      val result = for {
        userAnswers <- getOrRedirect(maybeUserAnswers, routes.RootController.displayPage)
        fileUploads <- getOrRedirect(userAnswers.fileUploadResponse, routes.ErrorPageController.error)
      } yield composeSuccessResult(fileUploads.uploads, userAnswers.mrn).map(Ok(_))

      result match {
        case Right(successResult) => successResult
        case Left(errorResult)    => errorResult
      }
    }
  }

  private def composeSuccessResult(
    uploads: List[FileUpload],
    maybeMrn: Option[MRN]
  )(implicit hc: HeaderCarrier, req: VerifiedEmailRequest[AnyContent]): Future[Html] =
    addFilenames(uploads).map { uploads =>
      uploadYourFilesConfirmation(uploads, maybeMrn, req.email)
    }

  private def getOrRedirect[A](option: Option[A], errorAction: Call): Either[Future[Result], A] =
    option.fold[Either[Future[Result], A]](Left(Future.successful(Redirect(errorAction))))(Right(_))

  private def addFilenames(uploads: List[FileUpload])(implicit hc: HeaderCarrier): Future[List[FileUpload]] =
    Future
      .sequence(uploads.map { upload =>
        val timer = metrics.startTimer(fetchNotificationMetric)
        cdsFileUploadConnector.getNotification(upload.reference).map { notificationOpt =>
          timer.stop()
          notificationOpt.map { notification =>
            upload.copy(filename = notification.filename.getOrElse(""))
          }
        }
      })
      .map(_.flatten)
}
