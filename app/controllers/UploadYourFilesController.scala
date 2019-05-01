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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UploadYourFilesController @Inject()(val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          requireEori: EORIRequiredActionImpl,
                                          getData: DataRetrievalAction,
                                          requireResponse: FileUploadResponseRequiredAction,
                                          dataCacheConnector: DataCacheConnector,
                                          upscanS3Connector: UpscanS3Connector,
                                          auditConnector: AuditConnector,
                                          implicit val appConfig: AppConfig,
                                          implicit val mat: Materializer) extends FrontendController with I18nSupport {

  private val auditSource = appConfig.appName
  private val audit = Audit(auditSource, auditConnector)

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      val references = req.fileUploadResponse.files.map(_.reference)
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.files.find(_.reference == ref) match {
        case Some(file) =>
          file.state match {
            case Waiting(_) => Ok(views.html.upload_your_files(ref, refPosition))
            case _ => Redirect(nextPage(file.reference, req.fileUploadResponse.files))
          }

        case None => Redirect(routes.SessionExpiredController.onPageLoad())
      }
    }

  def onSubmit(ref: String): Action[Either[MaxSizeExceeded, MultipartFormData[TemporaryFile]]] =
    (authenticate andThen requireEori andThen getData andThen requireResponse)
      .async(parse.maxLength(appConfig.fileFormats.maxFileSize, parse.multipartFormData)) { implicit req =>

        val files = req.fileUploadResponse.files

        files.find(_.reference == ref) match {
          case Some(file) =>
            file.state match {
              case Waiting(request) =>
                req.body match {
                  case Right(form) if form.file("file").exists(_.filename.nonEmpty) =>
                    val filename = form.file("file").get.filename
                    upscanS3Connector
                      .upload(request, form.file("file").get.ref, filename)
                      .flatMap { _ =>
                        val updatedFiles = file.copy(filename = filename) :: files.filterNot(_.reference == ref)
                        val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))
                        dataCacheConnector.save(answers.cacheMap)
                      }
                      .map(_ => Redirect(routes.UploadYourFilesController.onSuccess(ref)))

                  case Right(_) | Left(MaxSizeExceeded(_)) =>
                    Future.successful(Redirect(routes.UploadYourFilesController.onPageLoad(ref)))
                }

              case _ =>
                Future.successful(Redirect(nextPage(file.reference, req.fileUploadResponse.files)))
            }

          case None => Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
        }
      }

  def onSuccess(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val files = req.fileUploadResponse.files

      files.find(_.reference == ref) match {
        case Some(file) =>
          val updatedFiles = file.copy(state = Uploaded) :: files.filterNot(_.reference == ref)
          val answers: UserAnswers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))

          dataCacheConnector.save(answers.cacheMap).map { _ =>
            Redirect(nextPage(ref, files))
          }

        case None => Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
    }

  private def nextPage(ref: String, files: List[FileUpload])(implicit req: FileUploadResponseRequest[_]) = {
    val nextFileToUpload = files.collectFirst {
      case file@FileUpload(reference, Waiting(_), _) if reference > ref => file
    }

    nextFileToUpload match {
      case Some(file) => nextFile(file)
      case None => allFilesUploaded
    }
  }

  private def allFilesUploaded(implicit req: FileUploadResponseRequest[_]) = {
    auditUploadSuccess()
    routes.UploadYourFilesReceiptController.onPageLoad()
  }

  private def auditUploadSuccess()(implicit req: FileUploadResponseRequest[_]) = {
    def auditDetails = {
      val contactDetails = req.userAnswers.get(ContactDetailsPage).fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "emailAddress" -> cd.email, "telephoneNumber" -> cd.phoneNumber))
      val eori = Map("eori" -> req.request.eori)
      val mrn = req.userAnswers.get(MrnEntryPage).fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = req.userAnswers.get(HowManyFilesUploadPage).fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      //TODO      ListMap((1 to files.size).map(i => s"file$i").zip(files.map(_.reference)): _*)
      val references = req.fileUploadResponse.files.map(_.reference).foldLeft(Map.empty[String, String]) { (refs: Map[String, String], fileRef: String) => refs + ("fileReference" -> fileRef) }
      contactDetails ++ eori ++ mrn ++ numberOfFiles ++ references
    }

    sendDataEvent(transactionName = "trader-submission", detail = auditDetails, auditType = "UploadSuccess")
  }

  private def sendDataEvent(transactionName: String, path: String = "N/A", tags: Map[String, String] = Map.empty, detail: Map[String, String], auditType: String)(implicit hc: HeaderCarrier): Unit = {
    audit.sendDataEvent(DataEvent(
      auditSource,
      auditType,
      tags = hc.toAuditTags(transactionName, path) ++ tags,
      detail = hc.toAuditDetails(detail.toSeq: _*))
    )
  }

  private def nextFile(file: FileUpload): Call = routes.UploadYourFilesController.onPageLoad(file.reference)

  private def getPosition(ref: String, refs: List[String]) = refs match {
    case head :: tail if head == ref => First(refs.size)
    case init :+ last if last == ref => Last(refs.size)
    case _ => Middle(refs.indexOf(ref) + 1, refs.size)
  }
}

sealed trait Position

case class First(total: Int) extends Position

case class Middle(index: Int, total: Int) extends Position

case class Last(total: Int) extends Position
