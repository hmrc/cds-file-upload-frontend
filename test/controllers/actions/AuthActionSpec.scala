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

package controllers.actions

import config.ServiceUrls
import controllers.{routes, ControllerSpecBase}
import models.UnauthorisedReason.UserIsAgent
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.api.test._
import testdata.CommonTestData
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

class AuthActionSpec extends ControllerSpecBase {

  private lazy val serviceUrls = instanceOf[ServiceUrls]

  val authController: TestController = {
    val authAction = new AuthActionImpl(mockAuthConnector, mcc, serviceUrls)
    new TestController(authAction)
  }

  "AuthAction" should {

    "return authenticated user" in {
      val user = CommonTestData.signedInUser

      withSignedInUser(user) {

        val response = authController.action(FakeRequest())

        status(response) mustBe OK
        contentAsString(response) mustBe user.toString
      }
    }

    "redirect to gg sign in when NoActiveSession is returned" in {
      withAuthError(new NoActiveSession("") {}) {

        val request = FakeRequest("GET", "")
        val response = authController.action(request)

        status(response) mustBe SEE_OTHER
        redirectLocation(response).get must include("cds-file-upload-service")
      }
    }

    "redirect to Unauthorised" when {

      "authorisation fails" in {
        withAuthError(InsufficientEnrolments("")) {

          val response = authController.action(FakeRequest())

          status(response) mustBe SEE_OTHER
          redirectLocation(response) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }

      "user does not have an eori number" in {
        withUserWithoutEori {

          val response = authController.action(FakeRequest())

          status(response) mustBe SEE_OTHER
          redirectLocation(response) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "allow access" when {
      "the user has eori" in {

        val user = CommonTestData.signedInUser

        withSignedInUser(user) {
          val response = authController.action(FakeRequest())

          status(response) mustBe OK
        }
      }
    }

    "redirect to /you-cannot-use-this-service when user is an Agent" in {
      withSignedInAgent() {
        val result = authController.action(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onAgentKickOut(UserIsAgent).url)
      }
    }
  }

  class TestController(actions: AuthAction) extends FrontendController(mcc) {

    def action: Action[AnyContent] = actions.async { request =>
      Future.successful(Ok(request.user.toString))
    }
  }
}
