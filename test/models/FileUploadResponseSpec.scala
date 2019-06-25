/*
 * Copyright 2019 HM Revenue & Customs
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

package models

import base.SpecBase

class FileUploadResponseSpec extends SpecBase with XmlBehaviours {

  "File Upload Response" should {

    "parse xml" in {

      val xml = <FileUploadResponse xmlns="hmrc:fileupload">
        <Files>
          <File>
            <Reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://a.b.com</Href>
              <Fields>
                <Content-Type>application/xml</Content-Type>
                <x-amz-meta-callback-url>https://some-callback-url</x-amz-meta-callback-url>
                <x-amz-date>2019-03-05T11:56:34Z</x-amz-date>
                <success_action_redirect>https://success-redirect/abc-211</success_action_redirect>
                <error_action_redirect>https://error-redirect/abc-211</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
          <File>
            <Reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://x.y.com</Href>
              <Fields>
                <Content-Type>application/xml</Content-Type>
                <x-amz-meta-callback-url>https://some-callback-url2</x-amz-meta-callback-url>
                <x-amz-date>2019-03-05T11:57:11Z</x-amz-date>
                <success_action_redirect>https://success-redirect2/fyr-993</success_action_redirect>
                <error_action_redirect>https://error-redirect2/fyr-993</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
        </Files>
      </FileUploadResponse>

      val expectedResponse = FileUploadResponse(List(
        FileUpload(
          "31400000-8ce0-11bd-b23e-10b96e4ef00f",
          Waiting(UploadRequest(
            "https://a.b.com",
            Map(
              "Content-Type" -> "application/xml",
              "x-amz-meta-callback-url" -> "https://some-callback-url",
              "x-amz-date" -> "2019-03-05T11:56:34Z",
              "success_action_redirect" -> "https://success-redirect/abc-211",
              "error_action_redirect" -> "https://error-redirect/abc-211"
            )
          )),
          successUrl = RedirectUrl("https://success-redirect/abc-211"),
          errorUrl = RedirectUrl("https://error-redirect/abc-211"),
          id = "abc-211"
        ),
        FileUpload(
          "32400000-8cf0-11bd-b23e-10b96e4ef00f",
          Waiting(UploadRequest(
            "https://x.y.com",
            Map(
              "Content-Type" -> "application/xml",
              "x-amz-meta-callback-url" -> "https://some-callback-url2",
              "x-amz-date" -> "2019-03-05T11:57:11Z",
              "success_action_redirect" -> "https://success-redirect2/fyr-993",
              "error_action_redirect" -> "https://error-redirect2/fyr-993"
            )
          )),
          successUrl = RedirectUrl("https://success-redirect2/fyr-993"),
          errorUrl = RedirectUrl("https://error-redirect2/fyr-993"),
          id = "fyr-993"
          
        )
      ))


      FileUploadResponse.fromXml(xml) mustBe expectedResponse
    }
  }
}