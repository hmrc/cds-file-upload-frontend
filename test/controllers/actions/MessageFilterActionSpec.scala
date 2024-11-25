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

import controllers.ControllerSpecBase
import models.requests.{AuthenticatedRequest, MessageFilterRequest, VerifiedEmailRequest}
import models.{AllMessages, ExportMessages, SecureMessageAnswers}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.MockitoSugar.{mock, reset, when}
import play.api.mvc.Result
import services.SecureMessageAnswersService
import testdata.CommonTestData._

import scala.concurrent.Future

class MessageFilterActionSpec extends ControllerSpecBase {

  private val answersService: SecureMessageAnswersService = mock[SecureMessageAnswersService]
  private val action: ActionTestWrapper = new ActionTestWrapper(answersService)

  private val answers = SecureMessageAnswers(eori, ExportMessages)

  override def afterEach(): Unit = {
    reset(answersService)
    super.afterEach()
  }

  "MessageFilterAction" when {
    "the repository finds the user's filter selection" must {
      "build a SecureMessageAnswers object and add it to the MessageFilterRequest" in {
        when(answersService.findOne(eqTo(eori))) thenReturn Future.successful(Some(answers))
        val request = VerifiedEmailRequest(AuthenticatedRequest(fakeRequest, signedInUser), verifiedEmail)

        val result = action.callRefine(request).futureValue

        result.isRight mustBe true
        result.toOption.get.secureMessageAnswers mustBe answers
      }
    }

    "the repository does not find the user's filter selection" must {
      "return a default answer cache with no filter applied" in {
        when(answersService.findOne(eqTo(eori_2))) thenReturn Future.successful(None)
        val request = VerifiedEmailRequest(AuthenticatedRequest(fakeRequest, signedInUser.copy(eori = eori_2)), verifiedEmail)

        val result = action.callRefine(request).futureValue

        result.isRight mustBe true
        val secureMessageAnswer = result.toOption.get.secureMessageAnswers
        secureMessageAnswer.eori mustBe eori_2
        secureMessageAnswer.filter mustBe AllMessages
      }
    }
  }

  class ActionTestWrapper(answersService: SecureMessageAnswersService) extends MessageFilterActionImpl(answersService, mcc) {
    def callRefine[A](request: VerifiedEmailRequest[A]): Future[Either[Result, MessageFilterRequest[A]]] = refine(request)
  }
}
