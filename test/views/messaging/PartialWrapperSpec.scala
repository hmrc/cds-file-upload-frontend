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

import scala.collection.JavaConverters._

import base.{OverridableInjector, SpecBase}
import config.SecureMessagingConfig
import controllers.routes
import org.jsoup.nodes.{Document, Element}
import org.mockito.Mockito._
import org.scalatest.Assertion
import play.api.inject.bind
import play.twirl.api.HtmlFormat
import views.html.messaging.partial_wrapper
import views.matchers.ViewMatchers

class PartialWrapperSpec extends SpecBase with ViewMatchers {

  private val secureMessagingConfig = mock[SecureMessagingConfig]
  private val injector = new OverridableInjector(bind[SecureMessagingConfig].toInstance(secureMessagingConfig))

  private val partialWrapperPage = injector.instanceOf[partial_wrapper]
  private val partialContent = "Partial Content"

  override def afterEach(): Unit = {
    reset(secureMessagingConfig)
    super.afterEach()
  }

  "partial_wrapper in case of Conversation page" should {

    val titleKeyForConversation = "conversation.heading"
    val view = genView(titleKeyForConversation)

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage(titleKeyForConversation)
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display partial contents" in {
      view must containText(partialContent)
    }

    "display link for uploading files after having entered the MRN" in {
      assertUploadFilesLink(view)
    }
  }

  "partial_wrapper in case of Reply Result page" should {

    val titleKeyForReplyResult = "replyResult.heading"
    val view = genView(titleKeyForReplyResult)

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage(titleKeyForReplyResult)
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display partial contents" in {
      view must containText(partialContent)
    }

    "display link for uploading files after having entered the MRN" in {
      assertUploadFilesLink(view)
    }
  }

  private def assertUploadFilesLink(view: Document): Assertion = {
    val elements: List[Element] = view.getElementsByClass("govuk-link").iterator.asScala.toList
    assert(elements.exists { element =>
      element.text == messages("greyBox.uploadFiles") && element.attr("href") == routes.MrnEntryController.onPageLoad.url
    })
  }

  private def genView(titleKey: String): Document = {
    when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)
    partialWrapperPage(HtmlFormat.raw(partialContent), titleKey)(fakeRequest, messages)
  }
}
