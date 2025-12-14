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

import base.UnitSpec
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import services.AuditTypes.{FileUploaded, NavigateToMessages, UploadFailure, UploadSuccess}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends UnitSpec {
  private val mockAuditConnector = mock[AuditConnector]
  private val auditService = new AuditService(mockAuditConnector, appConfig)(global)
  private val auditFailure = Failure("Event sending failed")

  private val enrolment = AuthKey.enrolment
  private val eori = "GB150454489082"
  private val dcPath = "/secure-message-frontend/cds-file-upload-service/messages"
  private val contactDetails = ContactDetails("Joe Bloggs", "Bloggs Inc", "0123456")
  private val fileUploadCount = FileUploadCount(1)
  private val fileUpload = FileUpload("fileRef1", Successful, "", "id")

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockSendEvent()
  }

  "AuditService" should {

    "audit a 'NavigateToMessages' event" in {
      auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages, dcPath)(hc)
      verify(mockAuditConnector).sendExtendedEvent(ArgumentMatchers.refEq(navigateToMessageEvent, "eventId", "generatedAt"))(any(), any())
    }

    "audit a 'UploadSuccess' event with two explicit events (one legacy event and one in the new event style)" in {
      auditService.auditUploadResult(
        eori,
        Some(contactDetails),
        None,
        fileUploadCount,
        List(fileUpload),
        AuditTypes.UploadSuccess,
        appConfig.microservice.services.cdsFileUpload.fetchNotificationUri
      )(hc)
      verify(mockAuditConnector).sendEvent(ArgumentMatchers.refEq(uploadSuccessEventOldStyle, "eventId", "generatedAt"))(any(), any())
      verify(mockAuditConnector).sendEvent(ArgumentMatchers.refEq(uploadSuccessEventNewStyle, "eventId", "generatedAt"))(any(), any())
    }

    "audit a 'UploadFailure' event" in {
      auditService.auditUploadResult(
        eori,
        Some(contactDetails),
        None,
        fileUploadCount,
        List(fileUpload),
        AuditTypes.UploadFailure,
        appConfig.microservice.services.cdsFileUpload.fetchNotificationUri
      )(hc)
      verify(mockAuditConnector).sendEvent(ArgumentMatchers.refEq(uploadFailedEvent, "eventId", "generatedAt"))(any(), any())
    }

    "audit with a success" in {
      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages, dcPath)(hc).futureValue

      res mustBe Success
    }

    "handle audit failure" in {
      mockSendEvent(result = auditFailure)

      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages, dcPath)(hc).futureValue

      res mustBe auditFailure
    }

    "handled audit disabled" in {
      mockSendEvent(result = Disabled)

      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages, dcPath)(hc).futureValue

      res mustBe AuditResult.Disabled
    }
  }

  private val navigateToMessageEvent = ExtendedDataEvent(
    auditSource = appConfig.appName,
    auditType = NavigateToMessages.toString,
    detail = Json.parse("""{
                          |   "enrolment": "HMRC-CUS-ORG",
                          |   "eoriNumber": "GB150454489082",
                          |   "tags": {
                          |      "notificationType": "CDS-EXPORTS"
                          |   }
                          | }""".stripMargin),
    tags = AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags("callSFUSPartial", dcPath)
  )

  private val uploadSuccessEventOldStyle = DataEvent(
    auditSource = appConfig.appName,
    auditType = UploadSuccess.toString,
    detail = Map(
      "eori" -> eori,
      "telephoneNumber" -> contactDetails.phoneNumber,
      "fullName" -> contactDetails.name,
      "companyName" -> contactDetails.companyName,
      "numberOfFiles" -> fileUploadCount.get.value.toString,
      "fileReference1" -> fileUpload.reference
    ),
    tags = AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags("trader-submission", appConfig.microservice.services.cdsFileUpload.fetchNotificationUri)
  )

  private val uploadSuccessEventNewStyle = DataEvent(
    auditSource = appConfig.appName,
    auditType = FileUploaded.toString,
    detail = Map(
      "eori" -> eori,
      "phoneNumber" -> contactDetails.phoneNumber,
      "fullName" -> contactDetails.name,
      "companyName" -> contactDetails.companyName,
      "numberOfFiles" -> fileUploadCount.get.value.toString,
      "fileReference1" -> fileUpload.reference,
      "uploadResult" -> UploadSuccess.toString
    ),
    tags = AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags("trader-submission", appConfig.microservice.services.cdsFileUpload.fetchNotificationUri)
  )

  private val uploadFailedEvent = DataEvent(
    auditSource = appConfig.appName,
    auditType = FileUploaded.toString,
    detail = Map(
      "eori" -> eori,
      "phoneNumber" -> contactDetails.phoneNumber,
      "fullName" -> contactDetails.name,
      "companyName" -> contactDetails.companyName,
      "numberOfFiles" -> fileUploadCount.get.value.toString,
      "fileReference1" -> fileUpload.reference,
      "uploadResult" -> UploadFailure.toString
    ),
    tags = AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags("trader-submission", appConfig.microservice.services.cdsFileUpload.fetchNotificationUri)
  )

  private def mockSendEvent(result: AuditResult = Success) = {
    when(
      mockAuditConnector
        .sendExtendedEvent(ArgumentMatchers.any[ExtendedDataEvent])(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext])
    ).thenReturn(Future.successful(result))

    when(mockAuditConnector.sendEvent(ArgumentMatchers.any[DataEvent])(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext]))
      .thenReturn(Future.successful(result))
  }
}
