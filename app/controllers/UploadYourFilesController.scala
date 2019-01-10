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
import models.FileUploadResponse
import models.requests.FileUploadResponseRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files

@Singleton
class UploadYourFilesController @Inject()(
                                           val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           requireEori: EORIAction,
                                           getData: DataRetrievalAction,
                                           requireResponse: FileUploadResponseRequiredAction,
                                           implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      val callback    = getCallback(ref, req.fileUploadResponse.files.map(_.reference))
      val refPosition = getPosition(ref, req.fileUploadResponse.files.map(_.reference))

      req.fileUploadResponse.files.find(_.reference == ref) match {
        case Some(file) => Ok(upload_your_files(file.uploadRequest, callback, refPosition))
        case None       => Redirect(routes.SessionExpiredController.onPageLoad())
      }
  }

  def getCallback(ref: String, refs: List[String])(implicit request: Request[_]): String =
    refs
      .partition(_ <= ref)._2
      .headOption
      .map(routes.UploadYourFilesController.onPageLoad(_).absoluteURL())
      .getOrElse(routes.UploadYourFilesReceiptController.onPageLoad().absoluteURL())

  def getPosition(ref: String, refs: List[String]): Position =
    if (refs.headOption.contains(ref)) First
    else if (refs.lastOption.contains(ref)) Last
    else Middle
}

sealed trait Position
case object First  extends Position
case object Middle extends Position
case object Last   extends Position
