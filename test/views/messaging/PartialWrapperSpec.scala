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

package views.messaging

import controllers.routes
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import play.twirl.api.HtmlFormat
import views.base.UnitViewSpec
import views.html.messaging.partial_wrapper

import scala.jdk.CollectionConverters._

class PartialWrapperSpec extends UnitViewSpec {

  private val partialWrapperPage = instanceOf[partial_wrapper]
  private val partialContent = "Partial Content"

  "partial_wrapper when passed a backlink url" should {
 
    val titleKeyForConversation = "conversation.heading"
    val view = genView(titleKeyForConversation, true)

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage(titleKeyForConversation)
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "display the 'Back' link" in {
      assertBackLinkIsIncluded(view)
    }

    "display partial contents" in {
      view must containText(partialContent)
    }

    "display link for uploading files after having entered the MRN" in {
      assertUploadFilesLink(view)
    }
  }

  "partial_wrapper when not passed a backlink url" should {

    val titleKeyForReplyResult = "replyResult.heading"
    val view = genView(titleKeyForReplyResult, false)

    "display page header" in {
      view.getElementsByTag("title").first() must containMessage(titleKeyForReplyResult)
    }

    "display navigation banner" in {
      view must containElementWithID("navigation-banner")
    }

    "not display the 'Back' link" in {
      assertBackLinkIsNotIncluded(view)
    }

    "display partial contents" in {
      view must containText(partialContent)
    }

    "display link for uploading files after having entered the MRN" in {
      assertUploadFilesLink(view)
    }
  }

  "partial_wrapper when passed a positive error flag" should {
    val titleKeyForReplyResult = "replyResult.heading"
    val view = genView(titleKeyForReplyResult, None, true)

    "display 'Error' prefix in the title'" in {
      view.getElementsByTag("title").first().text() must startWith("Error:")
    }
  }

  private def assertUploadFilesLink(view: Document): Assertion = {
    val elements: List[Element] = view.getElementsByClass("govuk-link").iterator.asScala.toList
    assert(elements.exists { element =>
      element.text == messages("greyBox.uploadFiles") && element.attr("href") == routes.MrnEntryController.onPageLoad.url
    })
  }

  private def genView(titleKey: String, hasBoolean: Boolean): Document =
    partialWrapperPage(HtmlFormat.raw(partialContent), titleKey, routes.MrnEntryController.onPageLoad.url, hasBoolean)(request, messages)
}
