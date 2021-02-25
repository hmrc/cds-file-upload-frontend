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

import controllers.actions.{AuthAction, SecureMessagingFeatureAction, VerifiedEmailAction}
import forms.InboxChoiceForm
import forms.InboxChoiceForm.Values
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.inbox_choice_page

@Singleton
class InboxChoiceController @Inject()(
  mcc: MessagesControllerComponents,
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  secureMessagingFeatureAction: SecureMessagingFeatureAction,
  inboxChoicePage: inbox_choice_page
) extends FrontendController(mcc) with I18nSupport {

  val actions = authenticate andThen verifiedEmail andThen secureMessagingFeatureAction

  val onPageLoad: Action[AnyContent] = actions { implicit request =>
    Ok(inboxChoicePage(InboxChoiceForm.form))
  }

  val onSubmit: Action[AnyContent] = actions { implicit request =>
    InboxChoiceForm.form
      .bindFromRequest()
      .fold(formWithErrors => BadRequest(inboxChoicePage(formWithErrors)), _.choice match {
        case Values.ExportsMessages => Redirect(controllers.routes.SecureMessagingController.displayInbox)
        case Values.ImportsMessages => Redirect(controllers.routes.SecureMessagingController.displayInbox)
      })
  }
}
