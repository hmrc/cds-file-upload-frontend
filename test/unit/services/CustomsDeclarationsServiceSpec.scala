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

package services

import base.{SfusMetricsMock, SpecBase}
import connectors.CustomsDeclarationsConnector
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.MockitoSugar.{mock, reset, times, verify, when}
import org.scalatest._
import play.api.test.Helpers._

import scala.concurrent.Future

class CustomsDeclarationsServiceSpec extends SpecBase with BeforeAndAfterEach with SfusMetricsMock {

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

  ".batchFileUpload" must {

    "use eori number for the upload" in {
      await(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(5).get))
      verify(mockConnector).requestFileUpload(eqTo("GBEORINUMBER12345"), any())(any())
      verify(sfusMetrics, times(1)).incrementCounter(any())

    }
  }

  "use the mrn for the declaration id" in {
    await(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(1).get))
    verify(mockConnector).requestFileUpload(any(), eqTo(FileUploadRequest(MRN("13GB12345678901234").get, expectedUploadFiles(2))))(any())
    verify(sfusMetrics, times(1)).incrementCounter(any())
  }

  "have a max file sequence number as group size" in {
    val captor: ArgumentCaptor[FileUploadRequest] = ArgumentCaptor.forClass(classOf[FileUploadRequest])

    await(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(3).get))
    verify(mockConnector).requestFileUpload(any(), captor.capture())(any())

    val request = captor.getValue
    request.files.length mustBe request.files.map(_.fileSequenceNo).max
    verify(sfusMetrics, times(1)).incrementCounter(any())
  }

  "start file sequence number at 1" in {
    val captor: ArgumentCaptor[FileUploadRequest] = ArgumentCaptor.forClass(classOf[FileUploadRequest])

    await(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(4).get))
    verify(mockConnector).requestFileUpload(any(), captor.capture())(any())

    val request = captor.getValue
    request.files.map(_.fileSequenceNo).min mustBe 1
    verify(sfusMetrics, times(1)).incrementCounter(any())
  }

  "have init an upload request for an additional file for the contact details text" in {
    val captor: ArgumentCaptor[FileUploadRequest] = ArgumentCaptor.forClass(classOf[FileUploadRequest])
    val userUploadedFiles = 3

    await(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(userUploadedFiles).get))

    verify(mockConnector).requestFileUpload(any(), captor.capture())(any())
    captor.getValue.files.size mustBe userUploadedFiles + 1
    verify(sfusMetrics, times(1)).incrementCounter(any())
  }

  private def expectedUploadFiles(n: Int) = (1 to n).map(FileUploadFile(_, "", "http://localhost:6793").get)
}
