/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.actions.{AuthAction, DataRetrievalAction, FileUploadResponseRequiredAction, MrnRequiredAction, VerifiedEmailAction}
import metrics.SfusMetrics
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import services.{AuditService, FileUploadAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{upload_error, upload_multiple_files, upload_your_files}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadMultipleController @Inject()(
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
                                          uploadMultipleFilesView: upload_multiple_files,
                                          uploadError: upload_error
                                        )(implicit ec: ExecutionContext) 
  extends FrontendController(mcc) with I18nSupport with Logging {

  val actions = authenticate andThen verifiedEmail andThen getData andThen requireMrn andThen requireResponse

  def onPageLoad(): Action[AnyContent] = actions.async { implicit request =>
    Future.successful(Ok(uploadMultipleFilesView()(request)))
  }

}
