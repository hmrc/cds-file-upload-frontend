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

package base

import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded, AnyContentAsJson, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET

trait TestRequests {

  val getRequest: Request[AnyContent] = FakeRequest(GET, "/")

  def postRequest(body: JsValue): Request[AnyContentAsJson] =
    FakeRequest("POST", "").withJsonBody(body)

  def postRequestAsFormUrlEncoded(body: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
    FakeRequest("POST", "").withFormUrlEncodedBody(body: _*)
}
