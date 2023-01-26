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

import connectors.UpscanConnector
import controllers.actions._
import forms.FileUploadCountProvider
import models._
import models.requests.ContactDetailsRequest
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CustomsDeclarationsService, FileUploadAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.how_many_files_upload

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HowManyFilesUploadController @Inject() (
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireMrn: MrnRequiredAction,
  requireContactDetails: ContactDetailsRequiredAction,
  verifiedEmail: VerifiedEmailAction,
  formProvider: FileUploadCountProvider,
  answersService: FileUploadAnswersService,
  upscanConnector: UpscanConnector,
  customsDeclarationsService: CustomsDeclarationsService,
  mcc: MessagesControllerComponents,
  howManyFilesUpload: how_many_files_upload
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  val form = formProvider()

  val actions = authenticate andThen verifiedEmail andThen getData andThen requireMrn andThen requireContactDetails

  def onPageLoad: Action[AnyContent] = actions { implicit request =>
    val populatedForm = request.userAnswers.fileUploadCount.fold(form)(form.fill)
    Ok(howManyFilesUpload(populatedForm, request.request.mrn))
  }

  def onSubmit: Action[AnyContent] = actions.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(howManyFilesUpload(errorForm, request.request.mrn))),
        fileUploadCount =>
          uploadContactDetails(request, fileUploadCount) map {
            case Right(firstUpload :: _) =>
              logger.info("uploadContactDetails success: " + firstUpload)
              Redirect(routes.UpscanStatusController.onPageLoad(firstUpload.reference))

            case err =>
              logger.warn("uploadContactDetails error: " + err)
              Redirect(routes.ErrorPageController.error)
          }
      )
  }

  private def uploadContactDetails(request: ContactDetailsRequest[AnyContent], fileUploadCount: FileUploadCount)(
    implicit hc: HeaderCarrier
  ): Future[Either[Throwable, List[FileUpload]]] = {

    def saveRemainingFileUploadsToCache(fileUploadResponse: FileUploadResponse): Future[List[FileUpload]] = {
      val remainingFileUploads = fileUploadResponse.uploads.tail
      logger.info("remainingFileUploads " + remainingFileUploads)

      val answers =
        request.userAnswers.copy(fileUploadCount = Some(fileUploadCount), fileUploadResponse = Some(FileUploadResponse(remainingFileUploads)))

      answersService
        .findOneAndReplace(answers)
        .map { _ =>
          logger.info("saving remaining uploads")
          remainingFileUploads
        }
    }

    initiateUpload(request, fileUploadCount).flatMap { fileUploadResponse =>
      firstUploadFile(fileUploadResponse) match {
        case Right((_, uploadRequest)) =>
          upscanConnector.upload(uploadRequest, request.contactDetails).flatMap { response =>
            logger.info(s"Upload contact details successful: $response")
            logger.info(s"Upload contact details headers: ${response.header("Location")}")
            val isSuccessRedirect = response.header("Location").exists(_.contains("upscan-success"))
            if (response.status == SEE_OTHER && isSuccessRedirect) {
              saveRemainingFileUploadsToCache(fileUploadResponse).map(fileUploads => Right(fileUploads))
            } else {
              logger.warn(s"Left: error: illegal state")
              logger.warn(s"Response: $response")
              Future.successful(Left(new IllegalStateException("Contact details was not uploaded successfully")))
            }
          }
        case Left(error) =>
          logger.warn(s"Left: error: $error")
          Future.successful(Left(error))
      }
    }
  }

  private def initiateUpload(request: ContactDetailsRequest[AnyContent], fileUploadCount: FileUploadCount)(
    implicit hc: HeaderCarrier
  ): Future[FileUploadResponse] =
    customsDeclarationsService.batchFileUpload(request.eori, request.request.mrn, fileUploadCount)

  private def firstUploadFile(fileUploadResponse: FileUploadResponse): Either[Throwable, (FileUpload, UploadRequest)] =
    fileUploadResponse.uploads.headOption match {
      case Some(f @ FileUpload(_, Waiting(u), _, _)) => Right((f, u))
      case _                                         => Left(new IllegalStateException("Unable to initiate upload"))
    }
}
