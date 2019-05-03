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
import connectors.DataCacheConnector
import controllers.actions._
import forms.FileUploadCountProvider
import javax.inject.{Inject, Singleton}
import models._
import models.requests.MrnRequest
import pages.HowManyFilesUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CustomsDeclarationsService, UploadContactDetails}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HowManyFilesUploadController @Inject()(val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             requireEori: EORIRequiredActionImpl,
                                             getData: DataRetrievalAction,
                                             requireMrn: MrnRequiredAction,
                                             requireContactDetails: ContactDetailsRequiredAction,
                                             formProvider: FileUploadCountProvider,
                                             dataCacheConnector: DataCacheConnector,
                                             uploadContactDetails: UploadContactDetails,
                                             customsDeclarationsService: CustomsDeclarationsService,
                                             implicit val appConfig: AppConfig)(implicit ec: ExecutionContext) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireContactDetails andThen requireMrn) { implicit req =>

      val populatedForm =
        req.userAnswers
          .get(HowManyFilesUploadPage)
          .map(form.fill).getOrElse(form)

      Ok(views.html.how_many_files_upload(populatedForm))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireContactDetails andThen requireMrn).async { implicit req =>

      form.bindFromRequest().fold(
        errorForm => Future.successful(BadRequest(views.html.how_many_files_upload(errorForm))),

        fileUploadCount => {
          uploadContactDetails(req, fileUploadCount) map {
            case Right(firstFile) =>
              Redirect(routes.UploadYourFilesController.onPageLoad(firstFile.reference))
            case Left(_) =>
              Redirect(routes.SessionExpiredController.onPageLoad())
          }
        }
      )
    }

  private def uploadContactDetails(req: MrnRequest[AnyContent], fileUploadCount: FileUploadCount)(implicit hc: HeaderCarrier): Future[Either[Throwable, FileUpload]] = {
    def saveRemainingFileUploadsToCache(fileUploadResponse: FileUploadResponse): Future[List[FileUpload]] = {
      val remainingFileUploads = fileUploadResponse.files.tail
      val answers = updateUserAnswers(req.userAnswers, fileUploadCount, FileUploadResponse(remainingFileUploads))
      dataCacheConnector.save(answers.cacheMap).map { _ => remainingFileUploads }
    }

    initiateUpload(req, fileUploadCount).flatMap { fileUploadResponse =>
      firstUploadFile(fileUploadResponse) match {
        case Right((_, s3UploadRequest)) =>
          uploadContactDetails.upload(req.request.contactDetails, s3UploadRequest) match {
            case Right(success) => success.flatMap( _ => saveRemainingFileUploadsToCache(fileUploadResponse).map(uploads => Right(uploads.head)))
            case Left(e) => Future.successful(Left(e))
          }

        case Left(error) =>
          Future.successful(Left(error))
      }
    }
  }

  private def updateUserAnswers(userAnswers: UserAnswers, fileUploadCount: FileUploadCount, fileUploadResponse: FileUploadResponse) =
    userAnswers.set(HowManyFilesUploadPage, fileUploadCount).set(HowManyFilesUploadPage.Response, fileUploadResponse)

  private def initiateUpload(req: MrnRequest[AnyContent], fileUploadCount: FileUploadCount)(implicit hc: HeaderCarrier) =
    customsDeclarationsService.batchFileUpload(req.eori, req.mrn, fileUploadCount)

  private def firstUploadFile(response: FileUploadResponse): Either[Throwable, (FileUpload, UploadRequest)] =
    response.files.headOption map { case f@FileUpload(_, Waiting(u), _) => Right(f, u) } getOrElse Left(new IllegalStateException("Unable to initiate upload"))
}
