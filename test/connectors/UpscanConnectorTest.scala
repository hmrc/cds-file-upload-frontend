/*
 * Copyright 2022 HM Revenue & Customs
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

import scala.concurrent.Future

import akka.stream.scaladsl.Source
import akka.util.ByteString
import base.{SfusMetricsMock, SpecBase}
import models.{ContactDetails, UploadRequest}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.ws.ahc.AhcWSResponse
import play.api.libs.ws.ahc.cache.CacheableHttpResponseStatus
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.MultipartFormData
import play.shaded.ahc.org.asynchttpclient.Response
import play.shaded.ahc.org.asynchttpclient.uri.Uri

class UpscanConnectorTest extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with SpecBase with SfusMetricsMock {

  val uploadUrl = "http://localhost/theUploadUrl"
  val contactDetails = ContactDetails("a", "b", "c")

  "Upload" should {

    "POST to Upscan" in {
      val wsClient = mock[WSClient]
      val wsRequest = mock[WSRequest]

      implicit val materializer = app.materializer
      val connector = new UpscanConnector(appConfig, wsClient, sfusMetrics)

      when(wsRequest.withFollowRedirects(any())).thenReturn(wsRequest)
      when(wsClient.url(eqTo(uploadUrl))).thenReturn(wsRequest)

      val rb = new Response.ResponseBuilder()
      rb.accumulate(new CacheableHttpResponseStatus(Uri.create(uploadUrl), 303, "Everything is fine", "protocols"))
      val response = new AhcWSResponse(rb.build())
      when(wsRequest.post[Source[MultipartFormData.Part[Source[ByteString, _]], _]](any())(any())).thenReturn(Future.successful(response))

      val uploadRequest = UploadRequest(href = uploadUrl, fields = Map("key" -> "value"))
      val res = connector.upload(uploadRequest, contactDetails)
      res.futureValue.status mustBe Status.SEE_OTHER

      verify(sfusMetrics, times(1)).incrementCounter(any())
    }
  }
}
