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

package connectors

import config._
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import play.api.http.Status.CREATED
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BackendConnectorSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with OptionValues with BeforeAndAfterEach {

  private implicit val mockHeaderCarrier = mock[HeaderCarrier]

  private val mockAppConfig = mock[AppConfig]
  private val mockHttpClient = mock[HttpClient]
  private val emptyCustomsDeclarations = CustomsDeclarations(None, "", None, "", "")
  private val fileUpload = CDSFileUpload(Some("http"), "myhost", Some(1234))
  private val microservice = Microservice(Services(emptyCustomsDeclarations, fileUpload))
  
  private lazy val connector = new BackendConnector(mockAppConfig, mockHttpClient)

  override def beforeEach(): Unit = {
    reset(mockAppConfig, mockHttpClient)
    when(mockAppConfig.microservice).thenReturn(microservice)
  }

  "backend connector" should {

    "return previously saved data" in {

      val eori = EORI("GB12345644330")
      when(mockHttpClient.doPost(meq("http://myhost:1234/cds-file-upload/batch/GB12345644330"), any[BatchFileUpload], any[Seq[(String, String)]])(any[Writes[BatchFileUpload]], any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(CREATED)))
      val waiting = BatchFileUpload(MRN("12GB12345678901234").value, List(File("abc", Waiting(UploadRequest("abc", Map())))))
      val uploaded = BatchFileUpload(MRN("12GB12345678901234").value, List(File("def", Uploaded)))
      val virus = BatchFileUpload(MRN("12GB12345678901234").value, List(File("mno", VirusDetected)))
      when(mockHttpClient.GET(meq("http://myhost:1234/cds-file-upload/batch/GB12345644330"))(any[HttpReads[Option[List[BatchFileUpload]]]], any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(List(waiting, uploaded, virus))))

      val test = for {
        _ <- connector.save(eori, waiting)
        _ <- connector.save(eori, uploaded)
        _ <- connector.save(eori, virus)
        result <- connector.fetch(eori)
      } yield result

      test.futureValue mustBe Some(List(waiting, uploaded, virus))
    }
  }
}