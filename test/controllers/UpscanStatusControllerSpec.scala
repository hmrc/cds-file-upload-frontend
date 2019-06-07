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

package controllers

import controllers.actions.FileUploadResponseRequiredAction
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import connectors.Cache
import repositories.NotificationRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector


class UpscanStatusControllerSpec extends ControllerSpecBase {

  val cache = mock[Cache]
  val notificationRepository = mock[NotificationRepository]
  val auditConnector = mock[AuditConnector]

  val controller = new UpscanStatusController(
    messagesApi,
    new FakeAuthAction(),
    new FakeEORIAction(),
    fakeDataRetrievalAction(CacheMap("", Map.empty)),
    new FileUploadResponseRequiredAction(),
    cache,
    notificationRepository,
    auditConnector,
    appConfig)

  "Upscan Status error" should {

    "return error page" in {
      val result = controller.error("someId")(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) must include("An error occurred during upload")
    }
  }

}
