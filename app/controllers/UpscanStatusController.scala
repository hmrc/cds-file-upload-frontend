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

import config.AppConfig
import connectors.{CdsFileUploadConnector, CustomsDeclarationsConnector}
import controllers.actions._
import metrics.MetricIdentifiers.fetchNotificationMetric
import metrics.SfusMetrics
import models._
import models.requests.FileUploadResponseRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.AuditTypes.Audit
import services.{AuditService, AuditTypes, CustomsDeclarationsService, FileUploadAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{upload_error, upload_your_files}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanStatusController @Inject() (
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireMrn: MrnRequiredAction,
  verifiedEmail: VerifiedEmailAction,
  requireResponse: FileUploadResponseRequiredAction,
  answersService: FileUploadAnswersService,
  auditservice: AuditService,
  cdsFileUploadConnector: CdsFileUploadConnector,
  customsDeclarationsService: CustomsDeclarationsService,
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  implicit val appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  metrics: SfusMetrics,
  uploadYourFiles: upload_your_files,
  uploadError: upload_error
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  private val notificationsMaxRetries = appConfig.notifications.maxRetries
  private val notificationsRetryPause = appConfig.notifications.retryPauseMillis

  val actions = authenticate andThen verifiedEmail andThen getData andThen requireMrn andThen requireResponse

  val onPageLoad: Action[AnyContent] = actions.async { implicit request =>
    val files = request.fileUploadResponse.files
    val uploaded = uploadedFiles(files)

    if (uploaded.size >= FileUploadCount.maxNumberOfFiles)
      Future.successful(Ok(uploadYourFiles(None, request.request.mrn, uploaded)))
    else
      nextUploadSlot(files).map {
        case Some(uploadRequest) => Ok(uploadYourFiles(Some(uploadRequest), request.request.mrn, uploaded))
        case None                => Redirect(routes.ErrorPageController.error)
      }
  }

  // we need record each filename using JS. Currently, we can only retrieve the name using cdsFileUploadConnector.getNotification(reference)
  // but that name is only available at the end of the roundtrip. Instead, push name.
  // this name is then retrieved in uploadedFiles so it can be displayed
  def recordFilename(reference: String): Action[AnyContent] = actions.async { implicit request =>
    val filename = request.body.asFormUrlEncoded.flatMap(_.get("filename")).flatMap(_.headOption).getOrElse("")
    val files = request.fileUploadResponse.files

    if (filename.isEmpty || !files.exists(_.reference == reference)) Future.successful(NoContent)
    else {
      val updated = files.map(file => if (file.reference == reference) file.copy(filename = filename) else file)
      answersService
        .findOneAndReplace(request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(updated))))
        .map(_ => NoContent)
    }
  }

  def remove(reference: String): Action[AnyContent] = actions.async { implicit request =>
    val files = request.fileUploadResponse.files

    files.find(file => file.reference == reference && file.state == Uploaded) match {
      case Some(_) =>
        val remaining = files.filterNot(_.reference == reference)

        // TODO audit deletion
        answersService
          .findOneAndReplace(request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(remaining))))
          .map(_ => Redirect(routes.UpscanStatusController.onPageLoad))

      case None =>
        Future.successful(Redirect(routes.ErrorPageController.error))
    }
  }

  def error(reference: String): Action[AnyContent] = authenticate { implicit request =>
    Ok(uploadError())
  }

  def success(id: String): Action[AnyContent] = actions.async { implicit request =>
    val uploads = request.fileUploadResponse.files

    if (uploads.exists(_.id == id)) {
      val updatedFiles = uploads.map(file => if (file.id == id) file.copy(state = Uploaded) else file)
      val answers = request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(updatedFiles)))
      answersService.findOneAndReplace(answers).map(_ => Redirect(routes.UpscanStatusController.onPageLoad))
    } else
      Future.successful(Redirect(routes.ErrorPageController.error))
  }

  val finish: Action[AnyContent] = actions.async { implicit request =>
    val uploaded = request.fileUploadResponse.files.filter(_.state == Uploaded)

    if (uploaded.isEmpty)
      Future.successful(Redirect(routes.UpscanStatusController.onPageLoad))
    else
      request.userAnswers.batchId match {
        case None          =>
          Future.successful(Redirect(routes.ErrorPageController.error))
        case Some(batchId) =>
          //Send message to finish the batch
          completeBatch(batchId, uploaded)
      }
  }

  private def completeBatch(batchId: String, uploaded: List[FileUpload])(
    using request: FileUploadResponseRequest[_]
  ): Future[Result] =
    customsDeclarationsConnector.completeBatch(request.eori, batchId, uploaded.map(_.reference)).flatMap { _ =>
      allFilesUploaded(uploaded)
    }

  private def nextUploadSlot(files: List[FileUpload])(using request: FileUploadResponseRequest[_]): Future[Option[UploadRequest]] =
    files.lastOption match {
      case Some(FileUpload(_, Waiting(uploadRequest), _, _)) =>
        Future.successful(Some(uploadRequest))
      case _ =>
        request.userAnswers.batchId match {
          case None =>
            //TODO ???
            Future.successful(None)
          case Some(batchId) =>
            customsDeclarationsService.initiateSingleFileBatch(request.eori, request.request.mrn, batchId).flatMap { response =>
              response.files match {
                case (file @ FileUpload(_, Waiting(uploadRequest), _, _)) :: Nil =>
                  val answers = request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(files :+ file)))
                  answersService.findOneAndReplace(answers).map(_ => Some(uploadRequest))
                case other =>
                  logger.warn(s"Unexpected file: $other")
                  Future.successful(None)
              }
            }
        }
    }

  private def allFilesUploaded(uploads: List[FileUpload])(implicit request: FileUploadResponseRequest[_]): Future[Result] = {
    def failedUpload(notification: Notification): Boolean = notification.outcome != "SUCCESS"

    def prettyPrint: List[Notification] => String = _.map(n => s"(${n.fileReference}, ${n.outcome})").mkString(",")

    def retrieveNotifications(retries: Int = 0): Future[Result] = {
      val timer = metrics.startTimer(fetchNotificationMetric)

      val receivedNotifications = Future.sequence(uploads.map { upload =>
        cdsFileUploadConnector.getNotification(upload.reference)
      })

      val auditedPath = appConfig.microservice.services.cdsFileUpload.fetchNotificationUri

      receivedNotifications.flatMap { notifications =>
        timer.stop()
        notifications.flatten match {
          case ns if ns.exists(failedUpload) =>
            logger.warn("Failed notification received for an upload.")
            logger.warn(s"Notifications: ${prettyPrint(ns)}")

            auditUploadResult(uploads, AuditTypes.UploadFailure, auditedPath)

            clearUserCache(request.eori, request.userAnswers.uuid)
            Future.successful(Redirect(routes.ErrorPageController.uploadError))

          case ns if ns.length == uploads.length =>
            logger.info("All notifications successful.")

            auditUploadResult(uploads, AuditTypes.UploadSuccess, auditedPath)

            Future.successful(Redirect(routes.UploadYourFilesReceiptController.onPageLoad))

          case ns if retries < notificationsMaxRetries =>
            logger.info(
              s"Retrieved ${ns.length} of ${uploads.length} notifications. Retried $retries times. Retrying in $notificationsRetryPause ms ..."
            )
            Thread.sleep(notificationsRetryPause.toLong)
            retrieveNotifications(retries + 1)

          case ns =>
            logger.warn(s"Maximum number of retries exceeded. Retrieved ${ns.length} of ${uploads.length} notifications.")
            logger.warn(s"Notifications: ${prettyPrint(ns)}")

            auditUploadResult(uploads, AuditTypes.UploadFailure, auditedPath)

            clearUserCache(request.eori, request.userAnswers.uuid)
            Future.successful(Redirect(routes.ErrorPageController.uploadError))
        }
      }
    }

    retrieveNotifications()
  }

  private def clearUserCache(eori: String, uuid: String): Future[Unit] = answersService.remove(eori, uuid)

  private def uploadedFiles(files: List[FileUpload])(using messages: Messages): Seq[(String, String)] =
    files.collect { case FileUpload(reference, Uploaded, filename, _) => reference -> filename }
      .zipWithIndex
      .map { case ((reference, filename), index) =>
        val filenameMsg =
          if (filename.nonEmpty)
            filename
          else
            messages("fileUploadPage.uploadedFiles.unnamed", index + 1)
        reference -> filenameMsg
      }

  private def auditUploadResult(uploads: List[FileUpload], auditType: Audit, path: String)(
    implicit request: FileUploadResponseRequest[_],
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditservice.auditUploadResult(
      request.eori,
      request.userAnswers.contactDetails,
      request.userAnswers.mrn,
      FileUploadCount(uploads.size), // TODO remove this field from auditUploadResult and just apply uploads.size
      uploads,
      auditType,
      path
    )
}
