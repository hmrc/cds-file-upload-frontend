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

package controllers.actions

import controllers.ControllerSpecBase
import generators.SignedInUserGen
import models.UserAnswers
import models.requests.{AuthenticatedRequest, DataRequest, EORIRequest, SignedInUser, VerifiedEmailRequest}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import services.AnswersService

import scala.concurrent.Future

class DataRetrievalActionSpec extends ControllerSpecBase with SignedInUserGen {

  class Harness(answersConnector: AnswersService) extends DataRetrievalActionImpl(answersConnector, mcc) {
    def callTransform[A](request: VerifiedEmailRequest[A]): Future[DataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" when {
    "the connector finds data" must {
      "build a userAnswers object and add it to the request" in {

        forAll { (user: SignedInUser, eori: String, email: String) =>
          val answers = UserAnswers(eori)
          val answersConnector = mock[AnswersService]
          when(answersConnector.findOrCreate(eqTo(eori))) thenReturn Future.successful(answers)
          val action = new Harness(answersConnector)
          val request = VerifiedEmailRequest(EORIRequest(AuthenticatedRequest(fakeRequest, user), eori), email)

          val futureResult = action.callTransform(request)

          whenReady(futureResult) { result =>
            result.userAnswers mustBe answers
          }
        }
      }
    }
  }
}
