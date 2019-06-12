/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.Cache
import controllers.actions.{AuthAction, DataRetrievalAction, EORIRequiredAction, FileUploadResponseRequiredAction}
import javax.inject.Inject
import models._
import models.requests.FileUploadResponseRequest
import pages.{ContactDetailsPage, HowManyFilesUploadPage, MrnEntryPage}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsString
import play.api.mvc.{Action, AnyContent, Result}
import repositories.NotificationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_error

import scala.concurrent.{ExecutionContext, Future}

class UpscanStatusController @Inject()(val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       requireEori: EORIRequiredAction,
                                       getData: DataRetrievalAction,
                                       requireResponse: FileUploadResponseRequiredAction,
                                       cache: Cache,
                                       notificationRepository: NotificationRepository,
                                       auditConnector: AuditConnector,
                                       implicit val appConfig: AppConfig)(implicit ec: ExecutionContext) extends FrontendController with I18nSupport {

  private val AuditSource = appConfig.appName
  private val audit = Audit(AuditSource, auditConnector)
  private val notificationsMaxRetries = appConfig.notifications.maxRetries
  private val notificationsRetryPause = appConfig.notifications.retryPauseMillis

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val references = req.fileUploadResponse.uploads.map(_.reference)
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.uploads.find(_.reference == ref) match {
        case Some(upload) =>
          upload.state match {
            case Waiting(ur) => Future.successful(Ok(views.html.upload_your_files(ur, refPosition, upload.successUrl, upload.errorUrl)))
            case _ => nextPage(upload.reference, req.fileUploadResponse.uploads)
          }

        case None => 
          Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }
  
  def error(id: String): Action[AnyContent] =
    (authenticate andThen requireEori) { implicit req =>
      Ok(upload_error())
    }

  def success(id: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val uploads = req.fileUploadResponse.uploads

      uploads.find(_.id == id) match {
        case Some(upload) =>
          val updatedFiles = upload.copy(state = Uploaded) :: uploads.filterNot(_.id == id)
          val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))

          cache.save(answers.cacheMap).flatMap { _ =>
            nextPage(upload.reference, uploads)
          }

        case None =>
          Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }

  private def nextPage(ref: String, files: List[FileUpload])(implicit req: FileUploadResponseRequest[_]) = {
    def nextFile(file: FileUpload) = routes.UpscanStatusController.onPageLoad(file.reference)

    val nextFileToUpload = files.collectFirst {
      case file@FileUpload(reference, Waiting(_), _,  _, _, _) if reference > ref => file
    }

    nextFileToUpload match {
      case Some(file) =>
        Future.successful(Redirect(nextFile(file)))
      case None =>
        allFilesUploaded
    }
  }

  private def allFilesUploaded(implicit req: FileUploadResponseRequest[_]) = {
    def failedUpload(notification: Notification): Boolean = notification.outcome != "SUCCESS"

    def prettyPrint: List[Notification] => String = _.map(n => s"(${n.fileReference}, ${n.outcome})").mkString(",")

    val uploads = req.fileUploadResponse.uploads

    def retrieveNotifications(retries: Int = 0): Future[Result] = {
      val receivedNotifications = Future.sequence(
        uploads.map { upload =>
          notificationRepository.find("fileReference" -> JsString(upload.reference))
        }
      )

      receivedNotifications.flatMap { notifications =>
        notifications.flatten match {
          case ns if ns.exists(failedUpload) =>
            Logger.error("Failed notification received for an upload.")
            Logger.error(s"Notifications: ${prettyPrint(ns)}")
            Future.successful(Redirect(routes.ErrorPageController.uploadError()))

          case ns if ns.length == uploads.length =>
            Logger.debug("All notifications successful.")
            auditUploadSuccess()
            Future.successful(Redirect(routes.UploadYourFilesReceiptController.onPageLoad()))

          case ns if retries < notificationsMaxRetries =>
            Logger.debug(s"Retrieved ${ns.length} of ${uploads.length} notifications. Retrying in $notificationsRetryPause ms ...")
            Thread.sleep(notificationsRetryPause)
            retrieveNotifications(retries + 1)

          case ns =>
            Logger.error(s"Maximum number of retries exceeded. Retrieved ${ns.length} of ${uploads.length} notifications.")
            Logger.error(s"Notifications: ${prettyPrint(ns)}")
            Future.successful(Redirect(routes.ErrorPageController.uploadError()))
        }
      }
    }

    retrieveNotifications()
  }

  private def auditUploadSuccess()(implicit req: FileUploadResponseRequest[_]) = {
    def auditDetails = {
      val contactDetails = req.userAnswers.get(ContactDetailsPage).fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "emailAddress" -> cd.email, "telephoneNumber" -> cd.phoneNumber))
      val eori = Map("eori" -> req.request.eori)
      val mrn = req.userAnswers.get(MrnEntryPage).fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = req.userAnswers.get(HowManyFilesUploadPage).fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      val files = req.fileUploadResponse.uploads
      val fileReferences = (1 to files.size).map(i => s"fileReference$i").zip(files.map(_.reference)).toMap
      contactDetails ++ eori ++ mrn ++ numberOfFiles ++ fileReferences
    }

    sendDataEvent(transactionName = "trader-submission", detail = auditDetails, auditType = "UploadSuccess")
  }

  private def sendDataEvent(transactionName: String, path: String = "N/A", tags: Map[String, String] = Map.empty, detail: Map[String, String], auditType: String)(implicit hc: HeaderCarrier): Unit = {
    audit.sendDataEvent(DataEvent(
      AuditSource,
      auditType,
      tags = hc.toAuditTags(transactionName, path) ++ tags,
      detail = hc.toAuditDetails(detail.toSeq: _*))
    )
  }
  
  private def getPosition(ref: String, refs: List[String]) = refs match {
    case head :: tail if head == ref => First(refs.size)
    case init :+ last if last == ref => Last(refs.size)
    case _ => Middle(refs.indexOf(ref) + 1, refs.size)
  }
}