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
import connectors.CdsFileUploadConnector
import controllers.actions._
import metrics.MetricIdentifiers.fetchNotificationMetric
import metrics.SfusMetrics
import models._
import models.requests.FileUploadResponseRequest
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{AuditService, FileUploadAnswersService}
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

  def onPageLoad(reference: String): Action[AnyContent] = actions.async { implicit request =>
    val references = request.fileUploadResponse.uploads.map(_.reference)
    val refPosition = getPosition(reference, references)

    request.fileUploadResponse.uploads.find(_.reference == reference) match {
      case Some(upload) =>
        upload.state match {
          case Waiting(uploadRequest) => Future.successful(Ok(uploadYourFiles(uploadRequest, refPosition, request.request.mrn)))
          case _                      => nextPage(upload.reference, request.fileUploadResponse.uploads)
        }

      case None =>
        Future.successful(Redirect(routes.ErrorPageController.error))
    }
  }

  def error(reference: String): Action[AnyContent] = authenticate { implicit request =>
    Ok(uploadError())
  }

  def success(id: String): Action[AnyContent] = actions.async { implicit request =>
    val uploads = request.fileUploadResponse.uploads
    uploads.find(_.id == id) match {

      case Some(upload) =>
        val updatedFiles = upload.copy(state = Uploaded) :: uploads.filterNot(_.id == id)
        val answers = request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(updatedFiles)))
        answersService.findOneAndReplace(answers).flatMap { _ =>
          nextPage(upload.reference, uploads)
        }

      case None =>
        Future.successful(Redirect(routes.ErrorPageController.error))
    }
  }

  private def nextPage(ref: String, files: List[FileUpload])(implicit request: FileUploadResponseRequest[_]): Future[Result] = {
    def nextFile(file: FileUpload): Call = routes.UpscanStatusController.onPageLoad(file.reference)

    val nextFileToUpload = files.collectFirst {
      case file @ FileUpload(reference, Waiting(_), _, _) if reference > ref => file
    }

    nextFileToUpload match {
      case Some(file) =>
        Future.successful(Redirect(nextFile(file)))
      case None =>
        allFilesUploaded
    }
  }

  private def allFilesUploaded(implicit request: FileUploadResponseRequest[_]): Future[Result] = {
    def failedUpload(notification: Notification): Boolean = notification.outcome != "SUCCESS"

    def prettyPrint: List[Notification] => String = _.map(n => s"(${n.fileReference}, ${n.outcome})").mkString(",")

    val uploads = request.fileUploadResponse.uploads

    def retrieveNotifications(retries: Int = 0): Future[Result] = {
      val timer = metrics.startTimer(fetchNotificationMetric)

      val receivedNotifications = Future.sequence(uploads.map { upload =>
        cdsFileUploadConnector.getNotification(upload.reference)
      })

      receivedNotifications.flatMap { notifications =>
        timer.stop()
        notifications.flatten match {
          case ns if ns.exists(failedUpload) =>
            logger.warn("Failed notification received for an upload.")
            logger.warn(s"Notifications: ${prettyPrint(ns)}")
            clearUserCache(request.eori, request.userAnswers.uuid)
            Future.successful(Redirect(routes.ErrorPageController.uploadError))

          case ns if ns.length == uploads.length =>
            logger.info("All notifications successful.")
            auditservice.auditUploadSuccess(
              request.eori,
              request.userAnswers.contactDetails,
              request.userAnswers.mrn,
              request.userAnswers.fileUploadCount,
              request.fileUploadResponse.uploads
            )
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
            clearUserCache(request.eori, request.userAnswers.uuid)
            Future.successful(Redirect(routes.ErrorPageController.uploadError))
        }
      }
    }

    retrieveNotifications()
  }

  private def clearUserCache(eori: String, uuid: String): Future[Unit] = answersService.remove(eori, uuid)

  private def getPosition(ref: String, refs: List[String]): Position = refs match {
    case head :: _ if head == ref => First(refs.size)
    case _ :+ last if last == ref => Last(refs.size)
    case _                        => Middle(refs.indexOf(ref) + 1, refs.size)
  }
}
