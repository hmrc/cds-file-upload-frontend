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

package handlers

import base.UnitSpec
import config.ServiceUrls
import controllers.routes.UnauthorisedController
import models.AuthKey.enrolment
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.{HeaderNames, Status}
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, NoActiveSession}
import views.html.error_template

import scala.concurrent.ExecutionContext.global

class ErrorHandlerSpec extends UnitSpec {

  private val serviceUrls = mock[ServiceUrls]

  private val errorTemplate = instanceOf[error_template]
  private val errorHandler = new ErrorHandler(stubMessagesApi(), errorTemplate)(global, serviceUrls)

  "ErrorHandler.resolveError" should {

    "handle a NoActiveSession exception" in {
      when(serviceUrls.login).thenReturn("login-url")
      when(serviceUrls.loginContinue).thenReturn("login-continue-url")

      val result = errorHandler.resolveError(fakeRequest, new NoActiveSession("A user is not logged in") {}).futureValue

      result.header.status must be(Status.SEE_OTHER)
      result.header.headers.get(HeaderNames.LOCATION) must be(Some("login-url?continue=login-continue-url"))
    }

    "handle an InsufficientEnrolments exception" in {
      val result = errorHandler.resolveError(fakeRequest, InsufficientEnrolments(enrolment)).futureValue
      result.header.status must be(Status.SEE_OTHER)
      result.header.headers.get(HeaderNames.LOCATION) must be(Some(UnauthorisedController.onPageLoad.url))
    }
  }
}
