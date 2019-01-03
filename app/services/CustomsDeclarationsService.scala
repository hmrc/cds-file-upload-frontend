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

import com.google.inject.Inject
import connectors.CustomsDeclarationsConnector
import models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait CustomsDeclarationsService {

  def batchFileUpload(eori: String, mrn: MRN, fileUploadCount: FileUploadCount)(implicit hc: HeaderCarrier): Future[FileUploadResponse]

}

class CustomsDeclarationsServiceImpl @Inject()(customsDeclarationsConnector: CustomsDeclarationsConnector) extends CustomsDeclarationsService {

  override def batchFileUpload(eori: String, mrn: MRN, fileUploadCount: FileUploadCount)
                              (implicit hc: HeaderCarrier): Future[FileUploadResponse] = {

    val files = for(i <- 1 to fileUploadCount.value) yield FileUploadFile(i, "")

    FileUploadRequest(mrn.value, fileUploadCount.value, files.collect { case Some(v) => v }) match {
      case Some(request) => customsDeclarationsConnector.requestFileUpload(eori, request)
      case None          => ???
    }
  }

}