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
import models.exceptions.InvalidFeatureStateException
import models.InboxPartial
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.messaging.partial_wrapper

import scala.concurrent.Future

class SecureMessagingControllerSpec extends ControllerSpecBase with TestRequests {

  private val partialWrapperPage = mock[partial_wrapper]
  private val connector = mock[SecureMessageFrontendConnector]
  private val secureMessagingFeatureAction = new SecureMessagingFeatureActionMock()

  private val controller =
    new SecureMessagingController(
      new FakeAuthAction(),
      new FakeVerifiedEmailAction(),
      secureMessagingFeatureAction,
      connector,
      mcc,
      partialWrapperPage
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(partialWrapperPage)
    secureMessagingFeatureAction.reset()
    when(partialWrapperPage.apply(any[HtmlFormat.Appendable])(any[Request[_]], any[Messages])).thenReturn(HtmlFormat.empty)
  }

  "SecureMessagingController displayConversations is called" when {
    "feature flag for SecureMessaging is disabled" should {

      "throw InvalidFeatureStateException$" in {
        secureMessagingFeatureAction.disableSecureMessagingFeature()

        an[InvalidFeatureStateException] mustBe thrownBy {
          await(controller.displayConversations()(fakeRequest))
        }
      }
    }

    "feature flag for SecureMessaging is enabled" should {

      "call secure message connector" when {

        "successfully returns an InboxPartial" should {

          "wrap the partial in the inbox display wrapper" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()
            when(connector.retrieveConversationsPartial()(any(), any())).thenReturn(Future.successful(InboxPartial("")))

            val result = controller.displayConversations()(fakeRequest)

            status(result) mustBe OK
          }
        }

        "unsuccessfully returns a failed Future" should {

          "display the 'Sorry' page to the user" in {
            secureMessagingFeatureAction.enableSecureMessagingFeature()

            when(connector.retrieveConversationsPartial()(any(), any())).thenReturn(Future.failed(new Exception("Whoopse")))

            an[Exception] mustBe thrownBy { await(controller.displayConversations()(fakeRequest)) }
          }
        }
      }
    }
  }
}
