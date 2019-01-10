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

import com.google.inject.Singleton
import config.AppConfig
import connectors.DataCacheConnector
import controllers.actions._
import javax.inject.Inject
import models.{FileUploadResponse, Uploaded}
import models.requests.FileUploadResponseRequest
import pages.HowManyFilesUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files

import scala.concurrent.Future

@Singleton
class UploadYourFilesController @Inject()(
                                           val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           requireEori: EORIAction,
                                           getData: DataRetrievalAction,
                                           requireResponse: FileUploadResponseRequiredAction,
                                           dataCacheConnector: DataCacheConnector,
                                           implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      val references  = req.fileUploadResponse.files.map(_.reference)
      val callback    = routes.UploadYourFilesController.onSuccess(ref).absoluteURL()
      val refPosition = getPosition(ref, references)

      req.fileUploadResponse.files.find(_.reference == ref) match {
        case Some(file) => Ok(upload_your_files(file.uploadRequest, callback, refPosition))
        case None       => Redirect(routes.SessionExpiredController.onPageLoad())
      }
  }

  def onSuccess(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val references  = req.fileUploadResponse.files.map(_.reference)

      req.fileUploadResponse.files.find(_.reference == ref) match {
        case Some(file) => {
          val files = file.copy(state = Uploaded) :: req.fileUploadResponse.files.filterNot(_.reference == ref)
          val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, FileUploadResponse(files))

          dataCacheConnector.save(answers.cacheMap).map { _ =>
            Redirect(nextPage(ref, references))
          }
        }

        case None => Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
    }

  def nextPage(ref: String, refs: List[String])(implicit request: Request[_]): Call =
    refs
      .partition(_ <= ref)._2
      .headOption
      .map(routes.UploadYourFilesController.onPageLoad(_))
      .getOrElse(routes.UploadYourFilesReceiptController.onPageLoad())

  def getPosition(ref: String, refs: List[String]): Position =
    if (refs.headOption.contains(ref)) First
    else if (refs.lastOption.contains(ref)) Last
    else Middle
}

sealed trait Position

case object First  extends Position
case object Middle extends Position
case object Last   extends Position
