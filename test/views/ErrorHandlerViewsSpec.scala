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

package views

import base.UnitViewSpec
import handlers.ErrorHandler
import org.apache.pekko.stream.testkit.NoMaterializer
import views.html.error_template

class ErrorHandlerViewsSpec extends UnitViewSpec {

  private val errorHandler = instanceOf[ErrorHandler]
  private val errorTemplate = instanceOf[error_template]

  private val heading = messages("global.error.heading")
  private val message = messages("global.error.message")

  private val expectedPage = errorTemplate("global.error.title", heading, message)(request, messages)

  "ErrorHandler.defaultErrorTemplate" should {
    "be rendered as expected" in {
      val actualPage = errorHandler.defaultErrorTemplate()
      actualPage mustBe expectedPage
    }
  }

  "ErrorHandler.notFound" should {
    "be rendered as expected" in {
      val expectedPage = errorTemplate(
        "global.error.pageNotFound.title",
        messages("global.error.pageNotFound.heading"),
        messages("global.error.pageNotFound.message")
      )(request, messages).toString

      val result = errorHandler.notFound
      val actualPage = result.body.consumeData(NoMaterializer).futureValue.utf8String
      actualPage mustBe expectedPage
    }
  }

  "ErrorHandler.standardErrorTemplate" should {
    "be rendered as expected" in {
      val actualPage = errorHandler.standardErrorTemplate("global.error.title", heading, message)
      actualPage.futureValue mustBe expectedPage
    }
  }
}
