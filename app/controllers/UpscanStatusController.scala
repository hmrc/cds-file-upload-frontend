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

import config.AppConfig
import connectors.CdsFileUploadConnector
import controllers.actions.{AuthAction, DataRetrievalAction, EORIRequiredAction, FileUploadResponseRequiredAction, VerifiedEmailAction}

import javax.inject.Inject
import metrics.MetricIdentifiers.fetchNotificationMetric
import metrics.SfusMetrics
import models._
import models.requests.FileUploadResponseRequest
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.AnswersService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{upload_error, upload_your_files}

import scala.concurrent.{ExecutionContext, Future}

class UpscanStatusController @Inject()(
  authenticate: AuthAction,
  requireEori: EORIRequiredAction,
  getData: DataRetrievalAction,
  verifiedEmail: VerifiedEmailAction,
  requireResponse: FileUploadResponseRequiredAction,
  answersConnector: AnswersService,
  auditConnector: AuditConnector,
  cdsFileUploadConnector: CdsFileUploadConnector,
  implicit val appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  metrics: SfusMetrics,
  uploadYourFiles: upload_your_files,
  uploadError: upload_error
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  private val AuditSource = appConfig.appName
  private val audit = Audit(AuditSource, auditConnector)
  private val notificationsMaxRetries = appConfig.notifications.maxRetries
  private val notificationsRetryPause = appConfig.notifications.retryPauseMillis

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen verifiedEmail andThen getData andThen requireResponse).async { implicit req =>
      val references = req.fileUploadResponse.uploads.map(_.reference)
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.uploads.find(_.reference == ref) match {
        case Some(upload) =>
          upload.state match {
            case Waiting(ur) => Future.successful(Ok(uploadYourFiles(ur, refPosition)))
            case _           => nextPage(upload.reference, req.fileUploadResponse.uploads)
          }

        case None =>
          Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }

  def error(): Action[AnyContent] =
    (authenticate andThen requireEori) { implicit req =>
      Ok(uploadError())
    }

  def success(id: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen verifiedEmail andThen getData andThen requireResponse).async { implicit req =>
      val uploads = req.fileUploadResponse.uploads
      uploads.find(_.id == id) match {
        case Some(upload) =>
          val updatedFiles = upload.copy(state = Uploaded) :: uploads.filterNot(_.id == id)
          val answers = req.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(updatedFiles)))
          answersConnector.upsert(answers).flatMap { _ =>
            nextPage(upload.reference, uploads)
          }
        case None =>
          Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }

  private def nextPage(ref: String, files: List[FileUpload])(implicit req: FileUploadResponseRequest[_]): Future[Result] = {
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

  private def allFilesUploaded(implicit req: FileUploadResponseRequest[_]): Future[Result] = {
    def failedUpload(notification: Notification): Boolean = notification.outcome != "SUCCESS"

    def prettyPrint: List[Notification] => String = _.map(n => s"(${n.fileReference}, ${n.outcome})").mkString(",")

    val uploads = req.fileUploadResponse.uploads

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
            clearUserCache(req.eori)
            Future.successful(Redirect(routes.ErrorPageController.uploadError()))

          case ns if ns.length == uploads.length =>
            logger.info("All notifications successful.")
            auditUploadSuccess()
            Future.successful(Redirect(routes.UploadYourFilesReceiptController.onPageLoad()))

          case ns if retries < notificationsMaxRetries =>
            logger.info(s"Retrieved ${ns.length} of ${uploads.length} notifications. Retrying in $notificationsRetryPause ms ...")
            Thread.sleep(notificationsRetryPause)
            retrieveNotifications(retries + 1)

          case ns =>
            logger.warn(s"Maximum number of retries exceeded. Retrieved ${ns.length} of ${uploads.length} notifications.")
            logger.warn(s"Notifications: ${prettyPrint(ns)}")
            clearUserCache(req.eori)
            Future.successful(Redirect(routes.ErrorPageController.uploadError()))
        }
      }
    }

    retrieveNotifications()
  }

  private def clearUserCache(eori: String) = answersConnector.removeByEori(eori)

  private def auditUploadSuccess()(implicit req: FileUploadResponseRequest[_]): Unit = {
    def auditDetails: Map[String, String] = {
      val contactDetails = req.userAnswers.contactDetails
        .fold(Map.empty[String, String])(
          cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "emailAddress" -> cd.email, "telephoneNumber" -> cd.phoneNumber)
        )
      val eori = Map("eori" -> req.eori)
      val mrn = req.userAnswers.mrn.fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = req.userAnswers.fileUploadCount.fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      val files = req.fileUploadResponse.uploads
      val fileReferences = (1 to files.size).map(i => s"fileReference$i").zip(files.map(_.reference)).toMap
      contactDetails ++ eori ++ mrn ++ numberOfFiles ++ fileReferences
    }

    sendDataEvent(transactionName = "trader-submission", detail = auditDetails, auditType = "UploadSuccess")
  }

  private def sendDataEvent(
    transactionName: String,
    path: String = "N/A",
    tags: Map[String, String] = Map.empty,
    detail: Map[String, String],
    auditType: String
  )(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      DataEvent(AuditSource, auditType, tags = hc.toAuditTags(transactionName, path) ++ tags, detail = hc.toAuditDetails(detail.toSeq: _*))
    )

  private def getPosition(ref: String, refs: List[String]): Position = refs match {
    case head :: _ if head == ref => First(refs.size)
    case _ :+ last if last == ref => Last(refs.size)
    case _                        => Middle(refs.indexOf(ref) + 1, refs.size)
  }
}
