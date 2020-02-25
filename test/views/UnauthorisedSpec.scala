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

package views

import views.html.unauthorised

class UnauthorisedSpec extends DomAssertions {

  lazy val view = unauthorised()(fakeRequest, messages, appConfig).toString

  "view" should {

    "include header" in {
      view must include(messages("unauthorised.heading"))
    }

    "include title" in {
      view must include(messages("unauthorised.title"))
    }
  }
}
