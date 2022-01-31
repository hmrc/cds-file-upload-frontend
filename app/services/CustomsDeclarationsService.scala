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

import scala.concurrent.{ExecutionContext, Future}

import config.AppConfig
import connectors.CustomsDeclarationsConnector
import javax.inject.Inject
import metrics.MetricIdentifiers.fileUploadRequestMetric
import metrics.SfusMetrics
import models._
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

class CustomsDeclarationsService @Inject()(customsDeclarationsConnector: CustomsDeclarationsConnector, appConfig: AppConfig, metrics: SfusMetrics)(
  implicit ec: ExecutionContext
) extends Logging {

  def batchFileUpload(eori: String, mrn: MRN, fileUploadCount: FileUploadCount)(implicit hc: HeaderCarrier): Future[FileUploadResponse] = {

    val uploadUrl = appConfig.microservice.services.cdsFileUploadFrontend.uri
    logger.warn(s"uploadUrl: $uploadUrl")
    val files = for (i <- 1 to fileUploadCount.value + 1) yield FileUploadFile(i, "", uploadUrl)
    val fileSeq = files.flatten

    val request = FileUploadRequest(mrn, fileSeq)
    val timer = metrics.startTimer(fileUploadRequestMetric)

    customsDeclarationsConnector.requestFileUpload(eori, request).map { response =>
      timer.stop()
      metrics.incrementCounter(fileUploadRequestMetric)

      response
    }
  }
}
