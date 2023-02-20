/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.connectors

import base.SpecBase
import connectors.CustomsDeclarationsConnector
import models._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.MockitoSugar.{mock, reset, when}
import play.mvc.Http.Status.{BAD_REQUEST, OK}
import testdata.CommonTestData
import uk.gov.hmrc.http._

import scala.concurrent.Future

class CustomsDeclarationsConnectorSpec extends SpecBase {

  val httpClient = mock[HttpClient]

  val connector = new CustomsDeclarationsConnector(appConfig, httpClient)
  val clientId = "clientId"
  val conversationId = "conversationId"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient)
  }

  "CustomsDeclarationsConnector" when {
    "requestFileUpload is called" which {
      "receives a response body" which {
        "can be read to xml" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            val partialContent = "<div>Some Content</div>"
            val httpResponse = HttpResponse(status = OK, body = partialContent)

            when(httpClient.POSTString[HttpResponse](anyString(), any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(httpResponse))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest).futureValue

            result mustBe FileUploadResponse(List.empty)
          }
        }
        "cannot be read to xml" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")

            when(httpClient.POSTString[HttpResponse](anyString(), any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(httpResponse))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest)
            assert(result.failed.futureValue.isInstanceOf[org.xml.sax.SAXParseException])

          }
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            when(httpClient.POSTString[HttpResponse](anyString(), any(), any())(any(), any(), any()))
              .thenReturn(Future.failed(new BadGatewayException("Error")))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest)
            assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
          }
        }
      }
    }
  }

}
