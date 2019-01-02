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
import generators.SignedInUserGen
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import play.api.{Configuration, Environment}
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionSpec extends ControllerSpecBase
  with PropertyChecks
  with MockitoSugar
  with SignedInUserGen {

  lazy val conf = app.injector.instanceOf[Configuration]
  lazy val env  = app.injector.instanceOf[Environment]

  def authAction = new AuthActionImpl(authConnector, conf, env)

  def authController = new TestController(authAction)

  "AuthAction" should {

    "returned authenticated user" in {

      forAll { user: SignedInUser =>
        withSignedInUser(user) {

          val response = authController.action(FakeRequest())

          status(response) mustBe OK
          contentAsString(response) mustBe user.toString
        }
      }
    }

    "redirect to gg sign in" when {

      "NoActiveSession is returned" in {

        forAll { uri: String =>
          withAuthError(new NoActiveSession("") {}) {

            val request = FakeRequest().copyFakeRequest(uri = uri)
            val response = authController.action(request)

            status(response) mustBe SEE_OTHER
            redirectLocation(response) mustBe Some(s"/gg/sign-in?continue=${escaped(uri)}&origin=cds-file-upload-frontend")
          }
        }
      }
    }

    "redirect to Unauthorised" when {

      implicit val arbitraryAuthException: Arbitrary[AuthorisationException] = Arbitrary {
        Gen.oneOf(
          InsufficientEnrolments(""),
          InsufficientConfidenceLevel(""),
          UnsupportedAffinityGroup(""),
          UnsupportedAuthProvider(""),
          UnsupportedCredentialRole("")
        )
      }

      "authorisation fails" in {
        forAll { authException: AuthorisationException =>

          withAuthError(authException) {

            val response = authController.action(FakeRequest())

            status(response) mustBe SEE_OTHER
            redirectLocation(response) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }
    }
  }

  class TestController(actions: AuthAction) extends BaseController {

    def action: Action[AnyContent] = actions.async { request =>
      Future.successful(Ok(request.user.toString))
    }
  }
}