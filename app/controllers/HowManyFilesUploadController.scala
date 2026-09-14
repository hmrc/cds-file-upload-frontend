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

import connectors.UpscanConnector
import controllers.actions._
import models._
import models.requests.ContactDetailsRequest
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CustomsDeclarationsService, FileUploadAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HowManyFilesUploadController @Inject() (
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireMrn: MrnRequiredAction,
  requireContactDetails: ContactDetailsRequiredAction,
  verifiedEmail: VerifiedEmailAction,
  answersService: FileUploadAnswersService,
  upscanConnector: UpscanConnector,
  customsDeclarationsService: CustomsDeclarationsService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  val actions = authenticate andThen verifiedEmail andThen getData andThen requireMrn andThen requireContactDetails

  def onPageLoad: Action[AnyContent] = actions.async { implicit request =>
    uploadContactDetails(request).map {
      case Right(_) =>
        Redirect(routes.UpscanStatusController.onPageLoad)
      case Left(e) =>
        logger.warn("uploadContactDetails error: " + e)
        Redirect(routes.ErrorPageController.error)
    }
  }

  // TODO
  def onSubmit: Action[AnyContent] = Action(Redirect(routes.HowManyFilesUploadController.onPageLoad))

  private def uploadContactDetails(request: ContactDetailsRequest[AnyContent])(implicit hc: HeaderCarrier): Future[Either[Throwable, Unit]] =
    customsDeclarationsService.initiateSingleFileBatch(request.eori, request.request.mrn).flatMap { fileUploadResponse =>
      fileUploadResponse.files match {
        case FileUpload(_, Waiting(uploadRequest), _, _) :: Nil =>
          upscanConnector.upload(uploadRequest, request.contactDetails).flatMap { response =>
            val isSuccessRedirect = response.header("Location").exists(_.contains("upscan-success"))
            if (response.status == SEE_OTHER && isSuccessRedirect) {
              val answers = request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(Nil)))
              answersService.findOneAndReplace(answers).map(_ => Right(()))
            } else {
              logger.warn(s"Failed to upload contact details. Response: $response")
              Future.successful(Left(new IllegalStateException("Failed to upload contact details")))
            }
          }

        case other =>
          logger.warn(s"Unable to initiate upload. Expected file upload with waiting but got: $other")
          Future.successful(Left(new IllegalStateException("Unable to initiate upload")))
      }
    }
}
