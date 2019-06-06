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

import akka.stream.Materializer
import com.google.inject.Singleton
import config.AppConfig
import connectors.{DataCacheConnector, UpscanS3Connector}
import controllers.actions._
import javax.inject.Inject
import models._
import models.requests.FileUploadResponseRequest
import pages.{ContactDetailsPage, HowManyFilesUploadPage, MrnEntryPage}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsString
import play.api.mvc._
import repositories.NotificationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class UploadYourFilesController @Inject()(val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          requireEori: EORIRequiredAction,
                                          getData: DataRetrievalAction,
                                          requireResponse: FileUploadResponseRequiredAction,
                                          dataCacheConnector: DataCacheConnector,
                                          upscanS3Connector: UpscanS3Connector,
                                          auditConnector: AuditConnector,
                                          notificationRepository: NotificationRepository,
                                          implicit val appConfig: AppConfig,
                                          implicit val mat: Materializer) extends FrontendController with I18nSupport {

  private val MaxFileSizeInMB = appConfig.fileFormats.maxFileSizeMb
  private val FileTypes = appConfig.fileFormats.approvedFileTypes.split(',').map(_.trim)
  private val AuditSource = appConfig.appName
  private val audit = Audit(AuditSource, auditConnector)
  private val notificationsMaxRetries = appConfig.notifications.maxRetries
  private val notificationsRetryPause = appConfig.notifications.retryPauseMillis

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val references = req.fileUploadResponse.uploads.map(_.reference)
      val filenames = req.fileUploadResponse.uploads.map(_.filename).filter(_.nonEmpty)
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.uploads.find(_.reference == ref) match {
        case Some(file) =>
          file.state match {
            case Waiting(_) => Future.successful(Ok(views.html.upload_your_files(ref, refPosition, filenames)))
            case _ => nextPage(file.reference, req.fileUploadResponse.uploads)
          }

        case None => Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }

  def onSubmit(ref: String): Action[Either[MaxSizeExceeded, MultipartFormData[TemporaryFile]]] =
    (authenticate andThen requireEori andThen getData andThen requireResponse)
      .async(parse.maxLength(MaxFileSizeInMB * 1024 * 1024, parse.multipartFormData)) { implicit req =>

        val files = req.fileUploadResponse.uploads

        files.find(_.reference == ref) match {
          case Some(file) =>
            file.state match {
              case Waiting(request) =>
                req.body match {
                  case Right(form) if permittedFileType(form) =>
                    val Some((tempFile, filename)) = form.file("file") map (f => (f.ref, f.filename))

                    upscanS3Connector.upload(request, tempFile, filename) match {
                      case Success(_) =>
                        val updatedFiles = file.copy(filename = filename) :: files.filterNot(_.reference == ref)
                        val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))
                        dataCacheConnector.save(answers.cacheMap).map(_ => Redirect(routes.UploadYourFilesController.onSuccess(ref)))
                      case Failure(e) =>
                        Logger.error(s"Failed to upload file: $filename", e)
                        Future.successful(Redirect(routes.ErrorPageController.uploadError()))
                    }

                  case Left(MaxSizeExceeded(_)) =>
                    Future.successful(Redirect(routes.UploadYourFilesController.onPageLoad(ref)).flashing("fileUploadError" -> messagesApi.apply("fileUploadPage.validation.filesize", MaxFileSizeInMB)))
                  case _ =>
                    Future.successful(Redirect(routes.UploadYourFilesController.onPageLoad(ref)).flashing("fileUploadError" -> messagesApi.apply("fileUploadPage.validation.accept")))
                }

              case _ =>
                nextPage(file.reference, req.fileUploadResponse.uploads)
            }

          case None =>
            Future.successful(Redirect(routes.ErrorPageController.error()))
        }
      }

  private def permittedFileType(form: MultipartFormData[TemporaryFile]) = form.file("file").exists(_.contentType.exists(FileTypes.contains(_)))

  def onSuccess(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val files = req.fileUploadResponse.uploads

      files.find(_.reference == ref) match {
        case Some(file) =>
          val updatedFiles = file.copy(state = Uploaded) :: files.filterNot(_.reference == ref)
          val answers: UserAnswers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))

          dataCacheConnector.save(answers.cacheMap).flatMap { _ =>
            nextPage(ref, files)
          }

        case None => Future.successful(Redirect(routes.ErrorPageController.error()))
      }
    }

  private def nextPage(ref: String, files: List[FileUpload])(implicit req: FileUploadResponseRequest[_]) = {
    def nextFile(file: FileUpload) = routes.UploadYourFilesController.onPageLoad(file.reference)
    
    val nextFileToUpload = files.collectFirst {
      case file@FileUpload(reference, Waiting(_), _, _, _) if reference > ref => file
    }

    nextFileToUpload match {
      case Some(file) => Future.successful(Redirect(nextFile(file)))
      case None => allFilesUploaded
    }
  }


  def failedUpload(notification: Notification): Boolean = notification.outcome != "SUCCESS"

  private def allFilesUploaded(implicit req: FileUploadResponseRequest[_]) = {
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

  private def prettyPrint: List[Notification] => String = _.map(n => s"(${n.fileReference}, ${n.outcome})").mkString(",")

  private def auditUploadSuccess()(implicit req: FileUploadResponseRequest[_]) = {
    def auditDetails = {
      val contactDetails = req.userAnswers.get(ContactDetailsPage).fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "emailAddress" -> cd.email, "telephoneNumber" -> cd.phoneNumber))
      val eori = Map("eori" -> req.request.eori)
      val mrn = req.userAnswers.get(MrnEntryPage).fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = req.userAnswers.get(HowManyFilesUploadPage).fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      val files = req.fileUploadResponse.uploads
      val fileReferences = (1 to files.size).map(i => s"fileReference$i").zip(files.map(_.reference)).toMap
      val fileNames = (1 to files.size).map(i => s"fileName$i").zip(files.map(_.filename)).toMap
      contactDetails ++ eori ++ mrn ++ numberOfFiles ++ fileReferences ++ fileNames
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

