/*
 * Copyright 2020 HM Revenue & Customs
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

package base

import org.jsoup.nodes.Document
import org.scalatest.Assertion
import org.scalatest.matchers.{BeMatcher, MatchResult}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import views.matchers.ViewMatchers

class UnitViewSpec extends UnitSpec with ViewMatchers {

  import utils.FakeRequestCSRFSupport._

  implicit val request: Request[AnyContent] = FakeRequest().withCSRFToken

  protected implicit def messages(implicit request: Request[_]): Messages =
    new AllMessageKeysAreMandatoryMessages(realMessagesApi.preferred(request))

  protected def messages(key: String, args: Any*)(implicit request: Request[_]): String = messages(request)(key, args: _*)

  val realMessagesApi = UnitViewSpec.realMessagesApi

  def checkErrorsSummary(view: Document): Assertion = {
    view.getElementById("error-summary-heading").text() must be("error.summary.title")
    view.getElementsByClass("error-summary error-summary--show").get(0).getElementsByTag("p").text() must be("error.summary.text")
  }
}

class MessagesKeyMatcher(key: String) extends BeMatcher[String] {
  override def apply(left: String): MatchResult =
    if (left == key) {
      val missing = MessagesKeyMatcher.langs.find(lang => !UnitViewSpec.realMessagesApi.isDefinedAt(key)(lang))
      val language = missing.map(_.toLocale.getDisplayLanguage())
      MatchResult(
        missing.isEmpty,
        s"${language.getOrElse("None of languages")} does not have translation for $key",
        s"$key have translation for ${language.getOrElse("every language")}"
      )
    } else {
      MatchResult(matches = false, s"$left is not $key", s"$left is $key")
    }
}

object MessagesKeyMatcher {
  val langs: Seq[Lang] = Seq(Lang("en"))
}

object UnitViewSpec extends Injector {
  val realMessagesApi: MessagesApi = instanceOf[MessagesApi]
}
