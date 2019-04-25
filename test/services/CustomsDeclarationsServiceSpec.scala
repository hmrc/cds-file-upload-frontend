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

package services

import base.SpecBase
import connectors.CustomsDeclarationsConnector
import generators.Generators
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import models.{FileUploadCount, FileUploadRequest, FileUploadResponse, MRN}
import org.mockito.ArgumentCaptor
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CustomsDeclarationsServiceSpec extends SpecBase
  with MockitoSugar
  with PropertyChecks
  with Generators
  with BeforeAndAfterEach
  with ScalaFutures {

  lazy val connector = mock[CustomsDeclarationsConnector]
  lazy val service = new CustomsDeclarationsServiceImpl(connector)


  def capture[A](action: => Future[A])(f: (String, FileUploadRequest) => Unit): Future[Unit] = {

    val eoriCaptor = ArgumentCaptor.forClass(classOf[String])
    val fupCaptor = ArgumentCaptor.forClass(classOf[FileUploadRequest])

    action.map { _ =>
      verify(connector).requestFileUpload(eoriCaptor.capture(), fupCaptor.capture())(any())

      f(eoriCaptor.getValue, fupCaptor.getValue)
    }
  }


  override def beforeEach = {
    reset(connector)

    when(connector.requestFileUpload(any(), any())(any()))
      .thenReturn(Future.successful(FileUploadResponse(List())))
  }

  ".batchFileUpload" must {

    "not change eori number" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount) =>

        capture(service.batchFileUpload(eori, mrn, fileUploadCount)) {
          (newEori, _) =>

            newEori mustBe eori
        }
      }
    }

    "use the mrn for the declaration id" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount) =>

        capture(service.batchFileUpload(eori, mrn, fileUploadCount)) {
          (_, request) =>

            request.declarationId mustBe mrn
        }
      }
    }

    "have a max file sequence number as group size" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount) =>

        capture(service.batchFileUpload(eori, mrn, fileUploadCount)) {
          (_, request) =>

            request.files.length mustBe request.files.map(_.fileSequenceNo).max
        }
      }
    }

    "start file sequence number at 1" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount) =>

        capture(service.batchFileUpload(eori, mrn, fileUploadCount)) {
          (_, request) =>

            request.files.map(_.fileSequenceNo).min mustBe 1
        }
      }
    }

    "not have duplicate file sequence numbers" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount) =>

        capture(service.batchFileUpload(eori, mrn, fileUploadCount)) {
          (_, request) =>

            request.files.groupBy(_.fileSequenceNo) mustBe request.files
        }
      }
    }

    "have init an upload request for an additional file for the contact details text" in {
      val userUploadedFiles = 3

      capture(service.batchFileUpload("GBEORINUMBER12345", MRN("13GB12345678901234").get, FileUploadCount(userUploadedFiles).get)) {
        (_, request) =>
          verify(request.files.size mustBe userUploadedFiles + 15)
      }
    }

    "return the response from the connector" in {

      forAll { (eori: String, mrn: MRN, fileUploadCount: FileUploadCount, response: FileUploadResponse) =>

        when(connector.requestFileUpload(any(), any())(any()))
          .thenReturn(Future.successful(response))

        whenReady(service.batchFileUpload(eori, mrn, fileUploadCount)) { actual =>
          actual mustBe response
        }
      }
    }
  }
}