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

package connectors

import base.{IntegrationSpec, SfusMetricsMock}
import config.AppConfig
import models.{ContactDetails, UploadRequest}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.libs.ws.ahc.AhcWSResponse
import play.api.libs.ws.ahc.cache.CacheableHttpResponseStatus
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.MultipartFormData
import play.shaded.ahc.org.asynchttpclient.Response
import play.shaded.ahc.org.asynchttpclient.uri.Uri

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class UpscanConnectorISpec extends IntegrationSpec with SfusMetricsMock {

  "Upload" should {
    "POST to Upscan" in {
      val wsClient = mock[WSClient]
      val wsRequest = mock[WSRequest]

      val injector = fakeApplication().injector
      val connector = new UpscanConnector(injector.instanceOf[AppConfig], wsClient, sfusMetrics)(global, injector.instanceOf[Materializer])

      val uploadUrl = "http://localhost/theUploadUrl"

      when(wsRequest.withFollowRedirects(any())).thenReturn(wsRequest)
      when(wsClient.url(eqTo(uploadUrl))).thenReturn(wsRequest)

      val rb = new Response.ResponseBuilder()
      rb.accumulate(new CacheableHttpResponseStatus(Uri.create(uploadUrl), 303, "Everything is fine", "protocols"))
      val response = new AhcWSResponse(rb.build())
      when(wsRequest.post[Source[MultipartFormData.Part[Source[ByteString, _]], _]](any())(any())).thenReturn(Future.successful(response))

      val uploadRequest = UploadRequest(href = uploadUrl, fields = Map("key" -> "value"))
      val res = connector.upload(uploadRequest, ContactDetails("a", "b", "c"))
      res.futureValue.status mustBe Status.SEE_OTHER

      verify(sfusMetrics, times(1)).incrementCounter(any())
    }
  }
}
