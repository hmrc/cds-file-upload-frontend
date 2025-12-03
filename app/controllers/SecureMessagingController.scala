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

import connectors.SecureMessageFrontendConnector
import controllers.actions.{AuthAction, MessageFilterAction, VerifiedEmailAction}
import models.requests.MessageFilterRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import views.html.messaging.{inbox_wrapper, partial_wrapper}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

class SecureMessagingController @Inject() (
  authenticate: AuthAction,
  verifiedEmail: VerifiedEmailAction,
  messageFilterAction: MessageFilterAction,
  messageConnector: SecureMessageFrontendConnector,
  mcc: MessagesControllerComponents,
  inbox_wrapper: inbox_wrapper,
  partial_wrapper: partial_wrapper,
  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[MessageFilterRequest, AnyContent] = authenticate andThen verifiedEmail andThen messageFilterAction

  val displayInbox: Action[AnyContent] = actions.async { implicit request =>
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    val inboxHeader: String = defineInboxH1Text(request)
    val messages = Messages
    messageConnector
      .retrieveInboxPartial(request.request.eori, request.secureMessageAnswers.filter)
      .map { partial =>
        val originalStatus = """<span class="govuk-visually-hidden">Status</span>"""
        val replaceableStatus = s"""<span class="govuk-visually-hidden">${messages("common.status")}</span>"""
        val updatedBody = partial.body
          .replace(messages("inbox.original.heading"), inboxHeader)
          .replace(originalStatus, replaceableStatus)
        Ok(inbox_wrapper(HtmlFormat.raw(updatedBody), defineInboxH1Text(request)))
      }
  }

  def displayConversation(client: String, conversationId: String): Action[AnyContent] = actions.async { implicit request =>
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    messageConnector
      .retrieveConversationPartial(client, conversationId)
      .map(partial =>
        Ok(
          partial_wrapper(
            HtmlFormat.raw(partial.body),
            defineTitleText(partial.body),
            defineUploadLink(routes.SecureMessagingController.displayConversation(client, conversationId).url)
          )
        )
      )
  }

  def displayReplyResult(client: String, conversationId: String): Action[AnyContent] = actions.async { implicit request =>
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    messageConnector
      .retrieveReplyResult(client, conversationId)
      .map(partial =>
        Ok(
          partial_wrapper(
            HtmlFormat.raw(partial.body),
            "replyResult.heading",
            defineUploadLink(routes.SecureMessagingController.displayReplyResult(client, conversationId).url),
            hasBackButton = false
          )
        )
      )
  }

  def submitReply(client: String, conversationId: String): Action[AnyContent] = actions.async { implicit request =>
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    messageConnector
      .submitReply(client, conversationId, formData)
      .map {
        case None => Redirect(routes.SecureMessagingController.displayReplyResult(client, conversationId))
        case Some(partial) =>
          Ok(
            partial_wrapper(
              HtmlFormat.raw(partial.body),
              defineTitleText(partial.body),
              defineUploadLink(routes.SecureMessagingController.displayConversation(client, conversationId).url),
              hasErrors = true
            )
          )
      }
  }

  private def defineUploadLink(refererUrl: String) =
    routes.MrnEntryController.onPageLoad.url

  private def defineInboxH1Text(request: MessageFilterRequest[AnyContent])(implicit messages: Messages): String = {
    val inboxFilter = request.secureMessageAnswers.filter.toString
    inboxFilter match {
      case "ImportMessages" => messages("inbox.imports.heading")
      case "ExportMessages" => messages("inbox.exports.heading")
      case _                => messages("inbox.original.heading")
    }
  }

  private def defineTitleText(partialBody: String) = {
    val h1textPattern: Regex = "(?i)>(.*?)<\\/h1>".r
    val maybeMatcher = h1textPattern.findFirstMatchIn(partialBody)
    maybeMatcher.map(_.group(1)).getOrElse("")
  }

}
