/*
 * Copyright 2021 HM Revenue & Customs
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

import base.{Injector, UnitSpec}
import config.AppConfig
import models.{EORI, MRN, Notification, VerifiedEmailAddress}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.mvc.Http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import testdata.CommonTestData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class CdsFileUploadConnectorSpec extends UnitSpec with BeforeAndAfterEach with Injector with ScalaFutures {

  val appConfig = instanceOf[AppConfig]
  val httpClient = mock[HttpClient]
  val hc = mock[HeaderCarrier]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient)
  }

  override protected def afterEach(): Unit = {
    reset(httpClient)
    super.afterEach()
  }

  val cdsFileUploadConnector = new CdsFileUploadConnector(appConfig, httpClient)(global)

  "CdsFileUploadConnector on getNotification" should {

    "return notification" when {

      "notification exists" in {

        val notification = Notification("fileReference", "outcome", "fileName")

        when(httpClient.GET[Option[Notification]](anyString())(any(), any(), any()))
          .thenReturn(Future.successful(Some(notification)))

        val result = cdsFileUploadConnector.getNotification("fileReference")(hc).futureValue

        result mustBe Some(notification)
      }
    }

    "return None" when {

      "there is no notification with specific reference" in {

        when(httpClient.GET[Option[Notification]](anyString())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = cdsFileUploadConnector.getNotification("fileReference")(hc).futureValue

        result mustBe None
      }
    }
  }

  "CdsFileUploadConnector on getDeclarationStatus" should {

    "call HttpClient" in {

      val httpResponse = HttpResponse(status = OK, body = "")

      when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
        .thenReturn(Future.successful(httpResponse))

      cdsFileUploadConnector.getDeclarationStatus(MRN(CommonTestData.mrn).get)(hc).futureValue

      verify(httpClient).GET[HttpResponse](anyString())(any(), any(), any())
    }

    "return value returned from HttpClient" when {

      "response has Ok (200) status" in {

        val httpResponse = HttpResponse(status = OK, body = "")

        when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = cdsFileUploadConnector.getDeclarationStatus(MRN(CommonTestData.mrn).get)(hc).futureValue

        result mustBe httpResponse
      }

      "response has NotFound (404) status" in {

        val httpResponse = HttpResponse(status = NOT_FOUND, body = "")

        when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = cdsFileUploadConnector.getDeclarationStatus(MRN(CommonTestData.mrn).get)(hc).futureValue

        result mustBe httpResponse
      }

      "response has InternalServerError (500) status" in {

        val httpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = "")

        when(httpClient.GET[HttpResponse](anyString())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = cdsFileUploadConnector.getDeclarationStatus(MRN(CommonTestData.mrn).get)(hc).futureValue

        result mustBe httpResponse
      }
    }
  }

  "CdsFileUploadConnector on getVerifiedEmail" should {
    lazy val sampleEori = EORI("12345")

    "handle a 200 response by returning a VerifiedEmailAddress" in {
      val expectedVerifiedEmailAddress = VerifiedEmailAddress("some@email.com", ZonedDateTime.now())

      when(httpClient.GET[Option[VerifiedEmailAddress]](anyString())(any(), any(), any()))
        .thenReturn(Future.successful(Some(expectedVerifiedEmailAddress)))

      val result = cdsFileUploadConnector.getVerifiedEmailAddress(sampleEori)(hc).futureValue

      result mustBe Some(expectedVerifiedEmailAddress)
    }

    "handle a 404 response by returning None" in {
      when(httpClient.GET[Option[VerifiedEmailAddress]](anyString())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = cdsFileUploadConnector.getVerifiedEmailAddress(sampleEori)(hc).futureValue

      result mustBe None
    }

    "handle a 'non 404' 4XX response by throwing an exception" in {
      when(httpClient.GET[Option[VerifiedEmailAddress]](anyString())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 410)))

      val result = cdsFileUploadConnector.getVerifiedEmailAddress(sampleEori)(hc)

      assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
    }

    "handle a 5XX response by throwing an exception" ignore {
      when(httpClient.GET[Option[VerifiedEmailAddress]](anyString())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500)))

      val result = cdsFileUploadConnector.getVerifiedEmailAddress(sampleEori)(hc)

      assert(result.failed.futureValue.isInstanceOf[UpstreamErrorResponse])
    }
  }
}
