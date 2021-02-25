/*
 * Copyright 2021 HM Revenue & Customs
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

import config.SecureMessagingConfig
import controllers.actions.AuthAction
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.start

@Singleton
class StartController @Inject()(
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  start: start,
  secureMessagingConfig: SecureMessagingConfig
) extends FrontendController(mcc) with I18nSupport {

  val displayStartPage: Action[AnyContent] = Action { implicit req =>
    Ok(start())
  }

  def onStart: Action[AnyContent] = authenticate { _ =>
    if (secureMessagingConfig.isSecureMessagingEnabled)
      Redirect(controllers.routes.ChoiceController.onPageLoad())
    else
      Redirect(controllers.routes.MrnEntryController.onPageLoad())
  }

}
