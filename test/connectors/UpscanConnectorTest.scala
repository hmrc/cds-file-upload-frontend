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

//package connectors
//
//import base.SpecBase
//import com.github.tomakehurst.wiremock.client.WireMock._
//import models.{ContactDetails, UploadRequest}
//import play.api.http.Status
//import play.api.libs.ws.WSClient
//
//class UpscanConnectorTest extends WiremockTestServer  with SpecBase {
//
//  val wsClient = mock[WSClient]
//
//  private val connector = new UpscanConnector(appConfig, wsClient)
//  val contactDetails = ContactDetails("a","b","c","d")
//
//
//  "Upload" should {
//
//    "POST to Upscan" in {
//      stubFor(
//        post("/path")
//          .willReturn(
//            aResponse()
//              .withStatus(Status.SEE_OTHER)
//              .withHeader("Location", "upscan-success")
//          )
//      )
//
//      val templateUploading = UploadRequest(
//        href = s"$wireMockUrl/path",
//        fields = Map(
//          "key" -> "value"
//        )
//      )
//      val res = connector.upload(templateUploading, contactDetails)
//      res.futureValue mustBe Status.SEE_OTHER
//
//      verify(
//        postRequestedFor(urlEqualTo("/path"))
//      )
//    }
//
////    "Fail for error redirect" in {
////      stubFor(
////        post("/path")
////          .willReturn(
////            aResponse()
////              .withStatus(Status.SEE_OTHER)
////              .withHeader("Location", "error")
////          )
////      )
////
////      val templateUploading = UploadRequest(
////        href = s"$wireMockUrl/path",
////        fields = Map(
////          "key" -> "value"
////        )
////      )
////      val res = connector.upload(templateUploading, contactDetails)
////      res..failure.exception must have message "Uploading contact details to s3 failed"
////
////      verify(
////        postRequestedFor(urlEqualTo("/path"))
////      )
////    }
////
////    "Handle Bad Responses" in {
////      stubFor(
////        post("/path")
////          .willReturn(
////            aResponse()
////              .withStatus(Status.BAD_GATEWAY)
////              .withBody("content")
////          )
////      )
////
////      val templateUploading = UploadRequest(
////        href = s"$wireMockUrl/path",
////        fields = Map(
////          "key" -> "value"
////        )
////      )
////
////      val result = connector.upload(templateUploading, contactDetails)
////      result.failure.exception must have message "Uploading contact details to s3 failed"
////
////      verify(
////        postRequestedFor(urlEqualTo("/path"))
////      )
////    }
//
//  }
//
//}
//
