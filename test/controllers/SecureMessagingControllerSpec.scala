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

import base.TestRequests
import connectors.SecureMessageFrontendConnector
import models.{ConversationPartial, InboxPartial, ReplyResultPartial}
import models.exceptions.InvalidFeatureStateException
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeRequestCSRFSupport._
import views.html.messaging.{inbox_wrapper, partial_wrapper}

import scala.concurrent.Future

class SecureMessagingControllerSpec extends ControllerSpecBase with TestRequests {

  private val partialWrapperPage = mock[inbox_wrapper]
  private val partial_wrapper = mock[partial_wrapper]
  private val connector = mock[SecureMessageFrontendConnector]
  private val secureMessagingFeatureAction = new SecureMessagingFeatureActionMock()

  private val controller =
    new SecureMessagingController(
      new FakeAuthAction(),
      new FakeVerifiedEmailAction(),
      secureMessagingFeatureAction,
      new FakeMessageFilterAction(),
      connector,
      mcc,
      partialWrapperPage,
      partial_wrapper
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(partial_wrapper, partialWrapperPage, connector)
    secureMessagingFeatureAction.reset()

    when(partialWrapperPage.apply(any[HtmlFormat.Appendable])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
    when(partial_wrapper.apply(any[HtmlFormat.Appendable], any[String], any[Option[String]])(any[Request[_]], any[Messages]))
      .thenReturn(HtmlFormat.empty)
  }

  "SecureMessagingController displayInbox is called" when {
    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.displayInbox()(fakeRequest))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "call secure message connector" when {

        "successfully returns an InboxPartial" should {

          "wrap the partial in the inbox display wrapper" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()
            when(connector.retrieveInboxPartial(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(InboxPartial("")))

            val result = controller.displayInbox()(fakeRequest)

            status(result) mustBe OK
          }
        }

        "unsuccessfully returns a failed Future" should {

          "display the 'Sorry' page to the user" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            when(connector.retrieveInboxPartial(any(), any())(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("Whoopse")))

            an[Exception] mustBe thrownBy {
              await(controller.displayInbox()(fakeRequest))
            }
          }
        }
      }
    }
  }

  "SecureMessagingController displayConversation is called" when {
    val clientId = "clientId"
    val conversationId = "conversationId"

    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.displayConversation(clientId, conversationId)(fakeRequest.withCSRFToken))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "call secure message connector" when {

        "successfully returns a ConversationPartial" should {

          "wrap the partial in the conversation display wrapper" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()
            when(connector.retrieveConversationPartial(any[String], any[String])(any[HeaderCarrier]))
              .thenReturn(Future.successful(ConversationPartial("")))

            val result = controller.displayConversation(clientId, conversationId)(fakeRequest.withCSRFToken)

            status(result) mustBe OK
          }
        }

        "unsuccessfully returns a failed Future" should {

          "display the 'Sorry' page to the user" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            when(connector.retrieveConversationPartial(any[String], any[String])(any[HeaderCarrier]))
              .thenReturn(Future.failed(new Exception("Whoopse")))

            an[Exception] mustBe thrownBy {
              await(controller.displayConversation(clientId, conversationId)(fakeRequest.withCSRFToken))
            }
          }
        }
      }
    }
  }

  "SecureMessagingController displayReplyResult is called" when {
    val clientId = "clientId"
    val conversationId = "conversationId"

    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.displayReplyResult(clientId, conversationId)(fakeRequest))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "call secure message connector" that {

        "returns a ReplyResultPartial" should {

          "wrap the partial in the reply_result page" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()
            when(connector.retrieveReplyResult(any[String], any[String])(any[HeaderCarrier]))
              .thenReturn(Future.successful(ReplyResultPartial("")))

            val result = controller.displayReplyResult(clientId, conversationId)(fakeRequest)

            status(result) mustBe OK
          }
        }

        "returns a failed Future" should {

          "throw an exception" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            when(connector.retrieveReplyResult(any[String], any[String])(any[HeaderCarrier]))
              .thenReturn(Future.failed(new Exception("Whoopse")))

            an[Exception] mustBe thrownBy {
              await(controller.displayReplyResult(clientId, conversationId)(fakeRequest))
            }
          }
        }
      }
    }
  }

  "SecureMessagingController on submitReply" when {
    val clientId = "clientId"
    val conversationId = "conversationId"

    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.submitReply(clientId, conversationId)(fakeRequest.withCSRFToken))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "call secure message connector" in {
        secureMessagingFeatureAction.enableSecureMessagingFeature()
        when(connector.submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val postRequest = fakeRequest.withFormUrlEncodedBody("messageReply" -> "BlaBla").withCSRFToken
        controller.submitReply(clientId, conversationId)(postRequest).futureValue

        verify(connector).submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier])
      }

      "returns a successful Future of None" that {
        "wraps the returned partial in the partial_wrapper page" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()
          when(connector.submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(ConversationPartial("<html></html>"))))

          val postRequest = fakeRequest.withFormUrlEncodedBody("messageReply" -> "BlaBla").withCSRFToken
          val result = controller.submitReply(clientId, conversationId)(postRequest)

          status(result) mustBe OK
        }
      }

      "returns a successful Future of Some(ConversationPartial)" that {
        "wraps the returned partial in the partial_wrapper page" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()
          when(connector.submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(ConversationPartial("<html></html>"))))

          val postRequest = fakeRequest.withFormUrlEncodedBody("messageReply" -> "BlaBla").withCSRFToken
          val result = controller.submitReply(clientId, conversationId)(postRequest)

          status(result) mustBe OK
        }
      }

      "returns a failed Future" should {
        "return a 500 response to the user" in {
          secureMessagingFeatureAction.enableSecureMessagingFeature()
          when(connector.submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier]))
            .thenReturn(Future.failed(new Exception("Whoops!")))

          val postRequest = fakeRequest.withFormUrlEncodedBody("messageReply" -> "BlaBla").withCSRFToken

          an[Exception] mustBe thrownBy {
            await(controller.submitReply(clientId, conversationId)(postRequest))
          }

          verify(connector).submitReply(any[String], any[String], any[Map[String, Seq[String]]])(any[HeaderCarrier])
        }
      }
    }
  }
}
