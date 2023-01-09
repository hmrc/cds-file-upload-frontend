/*
 * Copyright 2023 HM Revenue & Customs
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
import config.AppConfig
import models._
import play.api.Logging
import play.api.libs.json.Json
import services.AuditTypes.{NavigateToMessages, UploadSuccess}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject()(connector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def auditSecureMessageInbox(enrolment: String, eori: String, filter: MessageFilterTag, path: String)(
    implicit hc: HeaderCarrier
  ): Future[AuditResult] = {
    val auditType = NavigateToMessages

    val extendedEvent = ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = auditType.toString,
      detail = Json.obj("enrolment" -> enrolment, "eoriNumber" -> eori, "tags" -> Json.obj("notificationType" -> filter.filterValue)),
      tags = getAuditTags("callSFUSPartial", path)
    )

    connector.sendExtendedEvent(extendedEvent).map(handleResponse(_, auditType.toString))
  }

  def auditUploadSuccess(
    eori: String,
    maybeContactDetails: Option[ContactDetails],
    maybeMrn: Option[MRN],
    maybeFileUploadCount: Option[FileUploadCount],
    uploads: List[FileUpload]
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val auditType = UploadSuccess

    def auditDetails: Map[String, String] = {
      val contactDetails = maybeContactDetails
        .fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "telephoneNumber" -> cd.phoneNumber))
      val eoriMap = Map("eori" -> eori)
      val mrn = maybeMrn.fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = maybeFileUploadCount.fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      val fileReferences = (1 to uploads.size)
        .map(i => s"fileReference$i")
        .zip(uploads.map(_.reference))
        .toMap

      contactDetails ++ eoriMap ++ mrn ++ numberOfFiles ++ fileReferences
    }

    val dataEvent = DataEvent(
      auditSource = appConfig.appName,
      auditType = auditType.toString,
      tags = getAuditTags("trader-submission", "N/A"),
      detail = hc.toAuditDetails(auditDetails.toSeq: _*)
    )

    connector.sendEvent(dataEvent).map(handleResponse(_, auditType.toString))
  }

  private def getAuditTags(transactionName: String, path: String)(implicit hc: HeaderCarrier) =
    AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags(transactionName, path)

  private def handleResponse(result: AuditResult, auditType: String) = result match {
    case Success =>
      logger.debug(s"Exports ${auditType} audit successful")
      Success
    case Failure(err, _) =>
      logger.warn(s"Exports ${auditType} Audit Error, message: $err")
      Failure(err)
    case Disabled =>
      logger.warn(s"Auditing Disabled")
      Disabled
  }
}

object AuditTypes extends Enumeration {
  type Audit = Value
  val NavigateToMessages, UploadSuccess = Value
}
