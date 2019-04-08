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
import models.{File, FileUploadResponse, Uploaded, Waiting}
import pages.HowManyFilesUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@Singleton
class UploadYourFilesController @Inject()(
                                           val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           requireEori: EORIAction,
                                           getData: DataRetrievalAction,
                                           requireResponse: FileUploadResponseRequiredAction,
                                           dataCacheConnector: DataCacheConnector,
                                           upscanS3Connector: UpscanS3Connector,
                                           implicit val appConfig: AppConfig,
                                           implicit val mat: Materializer) extends FrontendController with I18nSupport {

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      val references  = req.fileUploadResponse.files.map(_.reference)
      val callback    = routes.UploadYourFilesController.onSuccess(ref).absoluteURL()
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.files.find(_.reference == ref) match {
        case Some(file) =>
          file.state match {
            case Waiting(request) => Ok(upload_your_files(request, ref, callback, refPosition))
            case _                => Redirect(nextPage(file.reference, req.fileUploadResponse.files))
          }

        case None => Redirect(routes.SessionExpiredController.onPageLoad())
      }
  }

  def onSubmit(ref: String): Action[Either[MaxSizeExceeded, MultipartFormData[TemporaryFile]]] =
    (authenticate andThen requireEori andThen getData andThen requireResponse)
      .async(parse.maxLength(appConfig.fileFormats.maxFileSize, parse.multipartFormData)) { implicit req =>

        req.fileUploadResponse.files.find(_.reference == ref) match {
          case Some(file) =>
            file.state match {
              case Waiting(request) =>
                req.body match {
                  case Right(form) if form.file("file").exists(_.filename.nonEmpty) =>
                    upscanS3Connector
                      .upload(request, form.file("file").get.ref)
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

      val files  = req.fileUploadResponse.files

      files.find(_.reference == ref) match {
        case Some(file) =>
          val updatedFiles = file.copy(state = Uploaded) :: files.filterNot(_.reference == ref)
          val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(updatedFiles))

          dataCacheConnector.save(answers.cacheMap).map { _ =>
            Redirect(nextPage(ref, files))
          }

        case None => Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
    }

  def nextPage(ref: String, refs: List[File])(implicit request: Request[_]): Call =
    refs
      .partition(_.reference <= ref)._2
      .collectFirst { case file@File(_, Waiting(_)) => file }
      .map(file => routes.UploadYourFilesController.onPageLoad(file.reference))
      .getOrElse(routes.UploadYourFilesReceiptController.onPageLoad())

  def getPosition(ref: String, refs: List[String]): Position =
    if (refs.headOption.contains(ref)) First(refs.size)
    else if (refs.lastOption.contains(ref)) Last(refs.size)
    else Middle(refs.indexOf(ref) + 1, refs.size)
}

sealed trait Position

case class First(total: Int)              extends Position
case class Middle(index: Int, total: Int) extends Position
case class Last(total: Int)               extends Position
