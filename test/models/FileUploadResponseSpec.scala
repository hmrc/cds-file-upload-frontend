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
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._

class FileUploadResponseSpec extends SpecBase with XmlBehaviours {

  private val fileGen: Gen[FileUpload] = arbitrary[FileUpload].flatMap(file => arbitrary[Waiting].map(waiting => file.copy(state = waiting)))
  private val responseGen: Gen[FileUploadResponse] = listOfN(10, fileGen).map(files => FileUploadResponse(files))

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
                <success-action-redirect>https://success-redirect.com</success-action-redirect>
                <error-action-redirect>https://error-redirect.com</error-action-redirect>
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
                <x-amz-date>2019-03-04T11:56:34Z</x-amz-date>
                <success-action-redirect>https://success-redirect2.com</success-action-redirect>
                <error-action-redirect>https://error-redirect2.com</error-action-redirect>
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
              "x-amz-date" -> "2019-03-05T11:56:34Z"
            )
          )),
          successUrl = RedirectUrl("https://success-redirect.com"),
          errorUrl = RedirectUrl("https://error-redirect.com")
        ),
        FileUpload(
          "32400000-8cf0-11bd-b23e-10b96e4ef00f",
          Waiting(UploadRequest(
            "https://x.y.com",
            Map(
              "Content-Type" -> "application/xml",
              "x-amz-meta-callback-url" -> "https://some-callback-url2",
              "x-amz-date" -> "2019-03-05T11:56:34Z"
            )
          )),
          successUrl = RedirectUrl("https://success-redirect2.com"),
          errorUrl = RedirectUrl("https://error-redirect2.com")
        )
      ))
      
      FileUploadResponse.fromXml(xml) mustBe expectedResponse
    }
  }
}