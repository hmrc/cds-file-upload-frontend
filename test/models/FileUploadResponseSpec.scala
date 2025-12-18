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

package models

import base.UnitSpec
import play.api.libs.json.{JsValue, Json}

class FileUploadResponseSpec extends UnitSpec with XmlBehaviours {

  private val fileUpload1 =
    Json.parse("""{
        | "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
        | "state" : {
        |   "waiting" : {
        |     "href" : "https://a.b.com",
        |     "fields" : {
        |       "success_action_redirect" : "https://success-redirect/abc-211",
        |       "Content-Type" : "application/xml",
        |       "error_action_redirect" : "https://error-redirect/abc-211",
        |       "x-amz-meta-callback-url" : "https://some-callback-url",
        |       "x-amz-date" : "2019-03-05T11:56:34Z"
        |     }
        |   }
        | },
        | "filename" : "",
        | "id" : "abc-211"
        |}""".stripMargin)

  private val fileUpload2 = FileUpload(
    "32400000-8cf0-11bd-b23e-10b96e4ef00f",
    Waiting(
      UploadRequest(
        "https://x.y.com",
        Map(
          "Content-Type" -> "application/xml",
          "x-amz-meta-callback-url" -> "https://some-callback-url2",
          "x-amz-date" -> "2019-03-05T11:57:11Z",
          "success_action_redirect" -> "https://success-redirect2/fyr-993",
          "error_action_redirect" -> "https://error-redirect2/fyr-993"
        )
      )
    ),
    id = "fyr-993"
  )

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

      val expectedResponse = FileUploadResponse(List(fileUpload1.as[FileUpload], fileUpload2))

      FileUploadResponse.fromXml(xml) mustBe expectedResponse
    }
  }

  "A conversion from a 'FileUpload' instance" should {
    "generate the expected JSON" in {
      val json = Json.toJson(fileUpload1.as[FileUpload])
      json mustBe fileUpload1
    }
  }

  "A conversion from a 'FileUploadResponse' instance" should {
    "generate the expected JSON" in {
      val json = Json.toJson(FileUploadResponse(List(fileUpload1.as[FileUpload])))
      json mustBe Json.obj("files" -> List(fileUpload1))
    }
  }
}
