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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.UploadRequest
import org.scalatest.TryValues._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.Status
import play.api.libs.Files.TemporaryFile

class UpscanS3ConnectorTest extends WordSpec with WiremockTestServer with MustMatchers {

  private val connector = new UpscanConnector()


  "Upload" should {

    "POST to AWS" in {
      stubFor(
        post("/path")
          .willReturn(
            aResponse()
              .withStatus(Status.NO_CONTENT)
          )
      )

      val templateUploading = UploadRequest(
        href = s"$wireMockUrl/path",
        fields = Map(
          "key" -> "value"
        )
      )

      connector.upload(templateUploading, TemporaryFile("example-file.json"), "exampleFilename")

      verify(
        postRequestedFor(urlEqualTo("/path"))
      )
    }

    "Handle Bad Responses" in {
      stubFor(
        post("/path")
          .willReturn(
            aResponse()
              .withStatus(Status.BAD_GATEWAY)
              .withBody("content")
          )
      )

      val templateUploading = UploadRequest(
        href = s"$wireMockUrl/path",
        fields = Map(
          "key" -> "value"
        )
      )

      val result = connector.upload(templateUploading, TemporaryFile("example-file.json"), "exampleFileName")
      result.failure.exception must have message "Response code was not 303 but: 502"

      verify(
        postRequestedFor(urlEqualTo("/path"))
      )
    }

  }

}

