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

import config.ExternalServicesConfig
import controllers.actions.{AuthAction, EORIRequiredAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.unverified_email

import javax.inject.{Inject, Singleton}

@Singleton
class UnverifiedEmailController @Inject()(
  authenticate: AuthAction,
  requireEori: EORIRequiredAction,
  mcc: MessagesControllerComponents,
  unverified_email: unverified_email,
  config: ExternalServicesConfig
) extends FrontendController(mcc) with I18nSupport {

  val informUser: Action[AnyContent] = (authenticate andThen requireEori) { implicit req =>
    val redirectUrl = config.emailFrontendUrl
    Ok(unverified_email(redirectUrl))
  }
}
