/*
 * Copyright 2024 HM Revenue & Customs
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

import handlers.ErrorHandler
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.upload_error

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ErrorPageController @Inject() (mcc: MessagesControllerComponents, uploadErrorPage: upload_error, errorHandler: ErrorHandler)(
  implicit ec: ExecutionContext
) extends FrontendController(mcc) {

  def uploadError: Action[AnyContent] = Action { implicit request =>
    Ok(uploadErrorPage())
  }

  def error: Action[AnyContent] = Action.async { implicit request =>
    errorHandler.internalServerErrorTemplate.map(Ok(_))
  }
}
