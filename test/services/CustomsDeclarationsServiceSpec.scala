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

package services

import base.{SfusMetricsMock, UnitSpec}
import connectors.CustomsDeclarationsConnector
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers._

import scala.concurrent.Future

class CustomsDeclarationsServiceSpec extends UnitSpec with SfusMetricsMock {

  lazy val mockConnector = mock[CustomsDeclarationsConnector]
  lazy val service = new CustomsDeclarationsService(mockConnector, appConfig, sfusMetrics)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockConnector.requestFileUpload(any(), any())(any())).thenReturn(Future.successful(FileUploadResponse(List())))
  }

  override protected def afterEach(): Unit = {
    reset(mockConnector)

    super.afterEach()
  }

  ".initiateSingleFileBatch" must {

    "request a batch of exactly one file" in {
      val captor: ArgumentCaptor[FileUploadRequest] = ArgumentCaptor.forClass(classOf[FileUploadRequest])

      await(service.initiateSingleFileBatch("GBEORINUMBER12345", MRN("13GB12345678901234").get))
      verify(mockConnector).requestFileUpload(eqTo("GBEORINUMBER12345"), captor.capture())(any())

      val request = captor.getValue
      request.files.map(_.fileSequenceNo) mustBe Seq(1)
      (request.toXml \ "FileGroupSize").text mustBe "1"
      verify(sfusMetrics, times(1)).incrementCounter(any())
    }
  }

}
