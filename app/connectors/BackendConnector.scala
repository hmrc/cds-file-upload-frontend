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

import com.google.inject._
import config.AppConfig
import models.{BatchFileUpload, EORI}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackendConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)
                                (implicit ec: ExecutionContext) {

  private val fileUpload = appConfig.microservice.services.cdsFileUpload

  def save(eori: EORI, data: BatchFileUpload)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient.doPost(fileUpload.saveBatch(eori), data,  Seq("Content-Type" -> "application/json"))
  }

  def fetch(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[List[BatchFileUpload]]] = {
    httpClient.GET[Option[List[BatchFileUpload]]](fileUpload.getBatches(eori))
  }
}