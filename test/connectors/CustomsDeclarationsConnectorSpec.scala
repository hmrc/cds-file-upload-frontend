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

import generators.Generators
import models.{FileUploadRequest, FileUploadResponse}
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.mvc.Http.Status.{BAD_REQUEST, OK}
import testdata.CommonTestData
import uk.gov.hmrc.http.{BadGatewayException, HttpResponse}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class CustomsDeclarationsConnectorSpec extends ConnectorSpec with Generators {

  val connector = new CustomsDeclarationsConnector(appConfig, httpClient)(global)

  "CustomsDeclarationsConnector" when {
    "requestFileUpload is called" which {

      "receives a response body" which {

        "can be read to xml" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            val partialContent = "<div>Some Content</div>"
            val httpResponse = HttpResponse(status = OK, body = partialContent)
            when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest).futureValue

            result mustBe FileUploadResponse(List.empty)
          }
        }

        "cannot be read to xml" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            val httpResponse = HttpResponse(status = BAD_REQUEST, body = "")
            when(execute[HttpResponse]).thenReturn(Future.successful(httpResponse))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest)
            assert(result.failed.futureValue.isInstanceOf[org.xml.sax.SAXParseException])

          }
        }
      }

      "fails to connect to downstream service" should {
        "return a failed Future" in {
          forAll { (fileUploadRequest: FileUploadRequest) =>
            when(execute[HttpResponse]).thenReturn(Future.failed(new BadGatewayException("Error")))

            val result = connector.requestFileUpload(CommonTestData.eori, fileUploadRequest)
            assert(result.failed.futureValue.isInstanceOf[BadGatewayException])
          }
        }
      }
    }
  }
}
