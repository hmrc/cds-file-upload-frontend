/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions._
import javax.inject.Inject
import models.FileUpload
import play.api.i18n.I18nSupport
import play.api.libs.json.JsString
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.NotificationRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files_receipt

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadYourFilesReceiptController @Inject()(
  authenticate: AuthAction,
  requireEori: EORIRequiredAction,
  getData: DataRetrievalAction,
  requireResponse: FileUploadResponseRequiredAction,
  notificationRepository: NotificationRepository,
  uploadYourFilesReceipt: upload_your_files_receipt
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>
    addFilenames(req.fileUploadResponse.uploads).map { uploads =>
      Ok(uploadYourFilesReceipt(uploads))
    }
  }

  private def addFilenames(uploads: List[FileUpload]): Future[List[FileUpload]] =
    Future.sequence(uploads.map { u =>
      val filenameF = notificationRepository.find("fileReference" -> JsString(u.reference)).map(_.headOption.fold("")(_.filename))
      filenameF.map(f => u.copy(filename = f))
    })
}
