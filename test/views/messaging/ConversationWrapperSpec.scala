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

package views.messaging

import base.{OverridableInjector, SpecBase}
import config.SecureMessagingConfig
import org.jsoup.nodes.Document
import org.mockito.Mockito._
import play.api.inject.bind
import play.twirl.api.HtmlFormat
import views.html.messaging.conversation_wrapper
import views.matchers.ViewMatchers

class ConversationWrapperSpec extends SpecBase with ViewMatchers {

  private val secureMessagingConfig = mock[SecureMessagingConfig]
  private val injector = new OverridableInjector(bind[SecureMessagingConfig].toInstance(secureMessagingConfig))

  private val partialWrapperPage = injector.instanceOf[conversation_wrapper]
  private val partialContent = "Partial Content"

  private def view: Document = partialWrapperPage(HtmlFormat.raw(partialContent))(fakeRequest, messages)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(secureMessagingConfig)
    when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)
  }

  override def afterEach(): Unit = {
    reset(secureMessagingConfig)
    super.afterEach()
  }

  "Conversation Wrapper page" should {

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage("conversation.heading")
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display partial contents" in {
      view must containText(partialContent)
    }
  }
}
