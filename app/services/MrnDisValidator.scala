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

import connectors.CdsFileUploadConnector
import javax.inject.{Inject, Singleton}
import models.dis.DeclarationStatus
import models.{EORI, MRN}
import play.api.Logger
import play.api.libs.json.Json
import play.mvc.Http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MrnDisValidator @Inject()(cdsFileUploadConnector: CdsFileUploadConnector)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def validate(mrn: MRN, eori: EORI)(implicit hc: HeaderCarrier): Future[Boolean] =
    cdsFileUploadConnector.getDeclarationStatus(mrn).map { response =>
      response.status match {
        case OK        => validateDetails(mrn, eori, Json.parse(response.body).as[DeclarationStatus])
        case NOT_FOUND => false
        case otherStatus =>
          logger.warn(s"Declarations Information service responded with status: $otherStatus")
          throw new InternalServerException(s"Declarations Information service responded with status: $otherStatus")
      }
    }

  private def validateDetails(mrn: MRN, eori: EORI, declarationStatus: DeclarationStatus): Boolean =
    declarationStatus.eori == eori.value && declarationStatus.mrn == mrn.value

}
