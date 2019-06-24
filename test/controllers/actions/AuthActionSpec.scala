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

package controllers.actions

import controllers.ControllerSpecBase
import models.requests.SignedInUser
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.api.test._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.play.bootstrap.controller.{BackendController, BaseController}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends ControllerSpecBase {

  lazy val conf = app.injector.instanceOf[Configuration]
  lazy val env = app.injector.instanceOf[Environment]

  def authAction = new AuthActionImpl(mockAuthConnector, conf, env, mcc)

  def authController = new TestController(authAction)

  "AuthAction" should {

    "return authenticated user" in {

      val user = SignedInUser(Credentials("providerId", "providerType"), Name(Some("John"), Some("Doe")), Some("john@doe.com"), Some(Individual), "internalID", Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", "GB1234567890")), ""))))

      withSignedInUser(user) {

        val response = authController.action(FakeRequest())

        status(response) mustBe OK
        contentAsString(response) mustBe user.toString
      }
    }

    "redirect to gg sign in when NoActiveSession is returned" in {

      withAuthError(new NoActiveSession("") {}) {

        val myURI = "http://myservice:1234/somecontext"
        val request = FakeRequest().copyFakeRequest(uri = myURI)
        val response = authController.action(request)

        status(response) mustBe SEE_OTHER
        redirectLocation(response) mustBe Some(s"/gg/sign-in?continue=${escaped(myURI)}&origin=cds-file-upload-frontend")
      }
    }

    "redirect to Unauthorised when authorisation fails" in {

      withAuthError(InsufficientEnrolments("")) {

        val response = authController.action(FakeRequest())

        status(response) mustBe SEE_OTHER
        redirectLocation(response) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  class TestController(actions: AuthAction) extends BackendController(mcc) {

    def action: Action[AnyContent] = actions.async { request =>
      Future.successful(Ok(request.user.toString))
    }
  }

}