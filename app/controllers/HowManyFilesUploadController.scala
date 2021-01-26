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

import connectors.UpscanConnector
import controllers.actions._
import forms.FileUploadCountProvider
import javax.inject.{Inject, Singleton}
import models._
import models.requests.MrnRequest
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AnswersService, CustomsDeclarationsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.how_many_files_upload

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HowManyFilesUploadController @Inject()(
  authenticate: AuthAction,
  requireEori: EORIRequiredAction,
  getData: DataRetrievalAction,
  requireMrn: MrnRequiredAction,
  requireContactDetails: ContactDetailsRequiredAction,
  formProvider: FileUploadCountProvider,
  answersConnector: AnswersService,
  upscanConnector: UpscanConnector,
  customsDeclarationsService: CustomsDeclarationsService,
  mcc: MessagesControllerComponents,
  howManyFilesUpload: how_many_files_upload
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  val form = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireContactDetails andThen requireMrn) { implicit req =>
      val populatedForm =
        req.userAnswers.fileUploadCount.fold(form)(form.fill)

      Ok(howManyFilesUpload(populatedForm))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireContactDetails andThen requireMrn).async { implicit req =>
      form
        .bindFromRequest()
        .fold(
          errorForm => Future.successful(BadRequest(howManyFilesUpload(errorForm))),
          fileUploadCount => {
            uploadContactDetails(req, fileUploadCount) map {
              case Right(firstUpload :: _) =>
                logger.info("uploadContactDetails success: " + firstUpload)
                Redirect(routes.UpscanStatusController.onPageLoad(firstUpload.reference))
              case err =>
                logger.warn("uploadContactDetails error: " + err)
                Redirect(routes.ErrorPageController.error())
            }
          }
        )
    }

  private def uploadContactDetails(req: MrnRequest[AnyContent], fileUploadCount: FileUploadCount)(
    implicit hc: HeaderCarrier
  ): Future[Either[Throwable, List[FileUpload]]] = {
    def saveRemainingFileUploadsToCache(fileUploadResponse: FileUploadResponse): Future[List[FileUpload]] = {

      val remainingFileUploads = fileUploadResponse.uploads.tail
      logger.info("remainingFileUploads " + remainingFileUploads)

      answersConnector
        .upsert(req.userAnswers.copy(fileUploadCount = Some(fileUploadCount), fileUploadResponse = Some(FileUploadResponse(remainingFileUploads))))
        .map { _ =>
          logger.info("saving remaining uploads")

          remainingFileUploads
        }

    }

    initiateUpload(req, fileUploadCount).flatMap { fileUploadResponse =>
      firstUploadFile(fileUploadResponse) match {
        case Right((_, uploadRequest)) =>
          upscanConnector.upload(uploadRequest, req.request.contactDetails).flatMap { res =>
            logger.info(s"Upload contact details successful: $res")
            logger.info(s"Upload contact details headers: ${res.header("Location")}")
            val isSuccessRedirect = res.header("Location").exists(_.contains("upscan-success"))
            if (res.status == SEE_OTHER && isSuccessRedirect) {
              saveRemainingFileUploadsToCache(fileUploadResponse).map(uploads => Right(uploads))
            } else {
              logger.warn(s"Left: error: illegal state")
              logger.warn(s"Response: $res")
              Future.successful(Left(new IllegalStateException("Contact details was not uploaded successfully")))
            }
          }
        case Left(error) =>
          logger.warn(s"Left: error: $error")
          Future.successful(Left(error))
      }
    }
  }

  private def initiateUpload(req: MrnRequest[AnyContent], fileUploadCount: FileUploadCount)(implicit hc: HeaderCarrier): Future[FileUploadResponse] =
    customsDeclarationsService.batchFileUpload(req.eori, req.mrn, fileUploadCount)

  private def firstUploadFile(response: FileUploadResponse): Either[Throwable, (FileUpload, UploadRequest)] =
    response.uploads.headOption map { case f @ FileUpload(_, Waiting(u), _, _) => Right((f, u)) } getOrElse Left(
      new IllegalStateException("Unable to initiate upload")
    )
}
