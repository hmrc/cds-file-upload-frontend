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

import connectors.SecureMessageFrontendConnector
import controllers.actions.{AuthAction, SecureMessagingFeatureAction, VerifiedEmailAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.messaging.{conversation_wrapper, inbox_wrapper}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SecureMessagingController @Inject()(
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  secureMessagingFeatureAction: SecureMessagingFeatureAction,
  messageConnector: SecureMessageFrontendConnector,
  mcc: MessagesControllerComponents,
  inbox_wrapper: inbox_wrapper,
  conversation_wrapper: conversation_wrapper
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayInbox: Action[AnyContent] = (authenticate andThen verifiedEmail andThen secureMessagingFeatureAction).async { implicit req =>
    messageConnector
      .retrieveInboxPartial()
      .map { partial =>
        Ok(inbox_wrapper(HtmlFormat.raw(partial.body)))
      }
  }

  def displayConversation(client: String, conversationId: String): Action[AnyContent] =
    (authenticate andThen verifiedEmail andThen secureMessagingFeatureAction).async { implicit req =>
      messageConnector
        .retrieveConversationPartial(client, conversationId)
        .map { partial =>
          Ok(conversation_wrapper(HtmlFormat.raw(partial.body)))
        }
    }
}
