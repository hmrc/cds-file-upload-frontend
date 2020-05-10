/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.AnswersConnector
import controllers.ControllerSpecBase
import generators.SignedInUserGen
import models.UserAnswers
import models.requests.{AuthenticatedRequest, EORIRequest, OptionalDataRequest, SignedInUser}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._

import scala.concurrent.Future

class DataRetrievalActionSpec extends ControllerSpecBase with SignedInUserGen {

  class Harness(answersConnector: AnswersConnector) extends DataRetrievalActionImpl(answersConnector, mcc) {
    def callTransform[A](request: EORIRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" when {
    "the connector finds data" must {
      "build a userAnswers object and add it to the request" in {

        forAll { (user: SignedInUser, eori: String) =>
          val answers = UserAnswers(eori)
          val answersConnector = mock[AnswersConnector]
          when(answersConnector.findOrCreate(eqTo(eori))) thenReturn Future.successful(answers)
          val action = new Harness(answersConnector)
          val request = EORIRequest(AuthenticatedRequest(fakeRequest, user), eori)

          val futureResult = action.callTransform(request)

          whenReady(futureResult) { result =>
            result.userAnswers mustBe answers
          }
        }
      }
    }
  }
}
