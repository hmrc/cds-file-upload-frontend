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

import com.google.inject.Inject
import config.AppConfig
import models._
import models.requests.FileUploadResponseRequest
import play.api.Logging
import play.api.libs.json.Json
import services.AuditTypes.{Audit, FileUploaded, NavigateToMessages, UploadSuccess}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (connector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

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

  def auditUploadResult(request: FileUploadResponseRequest[_], auditType: Audit)(implicit hc: HeaderCarrier): Future[AuditResult] = {

    def auditDetails(isOldStyle: Boolean): Map[String, String] = {
      val phoneFieldKeyName = if (isOldStyle) "telephoneNumber" else "phoneNumber"

      val contactDetails = request.userAnswers.contactDetails
        .fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, phoneFieldKeyName -> cd.phoneNumber))
      val eoriMap = Map("eori" -> request.eori)
      val mrn = request.userAnswers.mrn.fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
      val numberOfFiles = request.userAnswers.fileUploadCount.fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
      val uploads = request.fileUploadResponse.uploads
      val fileReferences = (1 to uploads.size)
        .map(i => s"fileReference$i")
        .zip(uploads.map(_.reference))
        .toMap
      val uploadResult = if (!isOldStyle) Map("uploadResult" -> auditType.toString) else Map.empty

      contactDetails ++ eoriMap ++ mrn ++ numberOfFiles ++ fileReferences ++ uploadResult
    }

    def createDataEvent(isOldStyle: Boolean, auditType: Audit) = DataEvent(
      auditSource = appConfig.appName,
      auditType = auditType.toString,
      tags = getAuditTags("trader-submission", "N/A"),
      detail = hc.toAuditDetails(auditDetails(isOldStyle).toSeq: _*)
    )

    if (auditType == UploadSuccess) {
      connector.sendEvent(createDataEvent(true, UploadSuccess)).map(handleResponse(_, UploadSuccess.toString))
      connector.sendEvent(createDataEvent(false, FileUploaded)).map(handleResponse(_, FileUploaded.toString))
    } else {
      connector.sendEvent(createDataEvent(false, FileUploaded)).map(handleResponse(_, FileUploaded.toString))
    }
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
  val NavigateToMessages, UploadSuccess, UploadFailure, FileUploaded = Value
}
