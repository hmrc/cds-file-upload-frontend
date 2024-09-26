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

import base.{BaseSpec, Injector}
import config.AppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, reset, when}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

trait ConnectorSpec extends BaseSpec with Injector {

  val appConfig: AppConfig = instanceOf[AppConfig]

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  val httpClient: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]

  def execute[A]: Future[A] = requestBuilder.execute[A](any[HttpReads[A]], any[ExecutionContext])

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(httpClient.get(any[URL])(any())).thenReturn(requestBuilder)
    when(httpClient.post(any[URL])(any())).thenReturn(requestBuilder)
    when(requestBuilder.transform(any())).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[String])(any(), any(), any())).thenReturn(requestBuilder)
  }

  override protected def afterEach(): Unit = {
    reset(httpClient, requestBuilder)
    super.afterEach()
  }
}
