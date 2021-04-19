/*
 * Copyright 2021 HM Revenue & Customs
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
import models.ExportMessages
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.libs.json.Json
import services.AuditTypes.NavigateToMessages
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import uk.gov.hmrc.play.audit.AuditExtensions

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.global

class AuditServiceSpec extends SpecBase {
  private val mockAuditConnector = mock[AuditConnector]
  private val auditService = new AuditService(mockAuditConnector, appConfig)(global)
  private val auditFailure = Failure("Event sending failed")

  private val enrolment = "HMRC-CUS-ORG"
  private val eori = "GB150454489082"

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockSendEvent()
  }

  "AuditService" should {

    "audit a 'NavigateToMessages' event" in {
      auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages)(hc)
      verify(mockAuditConnector).sendExtendedEvent(ArgumentMatchers.refEq(extendedEvent, "eventId", "generatedAt"))(any(), any())
    }

    "audit with a success" in {
      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages)(hc).futureValue

      res mustBe Success
    }

    "handle audit failure" in {

      mockSendEvent(result = auditFailure)

      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages)(hc).futureValue

      res mustBe auditFailure
    }

    "handled audit disabled" in {

      mockSendEvent(result = Disabled)

      val res = auditService.auditSecureMessageInbox(enrolment, eori, ExportMessages)(hc).futureValue

      res mustBe AuditResult.Disabled
    }
  }

  private val extendedEvent = ExtendedDataEvent(
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
      .toAuditTags("callSFUSPartial", "/secure-message-frontend/cds-file-upload-service/messages")
  )

  private def mockSendEvent(result: AuditResult = Success) = {
    when(
      mockAuditConnector.sendExtendedEvent(ArgumentMatchers.any[ExtendedDataEvent])(
        ArgumentMatchers.any[HeaderCarrier],
        ArgumentMatchers.any[ExecutionContext]
      )
    ).thenReturn(Future.successful(result))
  }
}
