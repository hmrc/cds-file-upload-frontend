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

package services

import base.UnitSpec
import connectors.CdsFileUploadConnector
import models.{EORI, MRN}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.Json
import play.mvc.Http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import testdata.{CommonTestData, DeclarationStatusTestData}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MrnDisValidatorSpec extends UnitSpec with BeforeAndAfterEach with ScalaFutures with IntegrationPatience {

  implicit private val hc = mock[HeaderCarrier]
  private val cdsFileUploadConnector = mock[CdsFileUploadConnector]
  private val mrnDisValidator = new MrnDisValidator(cdsFileUploadConnector)

  private val defaultHttpResponse = HttpResponse(NOT_FOUND, "")
  private val mrn = MRN(CommonTestData.mrn).get
  private val eori = EORI(CommonTestData.eori)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(cdsFileUploadConnector)
    when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(defaultHttpResponse))
  }

  override protected def afterEach(): Unit = {
    reset(cdsFileUploadConnector)
    super.afterEach()
  }

  "MrnDisValidator on validate" should {

    "call CdsFileUploadConnector" in {

      mrnDisValidator.validate(mrn, eori).futureValue

      verify(cdsFileUploadConnector).getDeclarationStatus(meq(mrn))(any())
    }

    "return false" when {

      "CdsFileUploadConnector returns NotFound (404) response" in {

        val response = HttpResponse(NOT_FOUND, "")
        when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(response))

        val result = mrnDisValidator.validate(mrn, eori).futureValue

        result mustBe false
      }

      "CdsFileUploadConnector returns Ok (200) response" that {

        "contains DeclarationStatus with different EORI" in {

          val declarationStatus = DeclarationStatusTestData.declarationStatus.copy(eori = CommonTestData.eori_2)
          val response = HttpResponse(status = OK, json = Json.toJson(declarationStatus), headers = Map.empty)
          when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(response))

          val result = mrnDisValidator.validate(mrn, eori).futureValue

          result mustBe false
        }

        "contains DeclarationStatus with different MRN" in {

          val declarationStatus = DeclarationStatusTestData.declarationStatus.copy(mrn = CommonTestData.mrn_2)
          val response = HttpResponse(status = OK, json = Json.toJson(declarationStatus), headers = Map.empty)
          when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(response))

          val result = mrnDisValidator.validate(mrn, eori).futureValue

          result mustBe false
        }
      }
    }

    "return true" when {

      "CdsFileUploadConnector returns Ok (200) response" that {

        "contains DeclarationStatus with correct EORI and MRN" in {

          val declarationStatus = DeclarationStatusTestData.declarationStatus
          val response = HttpResponse(status = OK, json = Json.toJson(declarationStatus), headers = Map.empty)
          when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(response))

          val result = mrnDisValidator.validate(mrn, eori).futureValue

          result mustBe true
        }
      }
    }

    "throw an exception" when {

      "CdsFileUploadConnector returns InternalServerError (500) response" in {
        val response = HttpResponse(INTERNAL_SERVER_ERROR, "Internal server error")
        when(cdsFileUploadConnector.getDeclarationStatus(any[MRN])(any())).thenReturn(Future.successful(response))

        an[Exception] mustBe thrownBy {
          mrnDisValidator.validate(mrn, eori).futureValue
        }
      }
    }
  }
}
