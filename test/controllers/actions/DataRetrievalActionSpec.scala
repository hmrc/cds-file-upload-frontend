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
import models.UserAnswers
import models.requests.{AuthenticatedRequest, DataRequest, VerifiedEmailRequest}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import services.AnswersService
import testdata.CommonTestData._

import scala.concurrent.Future

class DataRetrievalActionSpec extends ControllerSpecBase {

  private val answersConnector: AnswersService = mock[AnswersService]
  private val action: ActionTestWrapper = new ActionTestWrapper(answersConnector)

  private val answers = UserAnswers(eori)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(answersConnector)
    when(answersConnector.findOrCreate(eqTo(eori))) thenReturn Future.successful(answers)
  }

  override def afterEach(): Unit = {
    reset(answersConnector)
    super.afterEach()
  }

  "Data Retrieval Action" when {
    "the connector finds data" must {
      "build a userAnswers object and add it to the request" in {

        val request = VerifiedEmailRequest(AuthenticatedRequest(fakeRequest, signedInUser), verifiedEmail)

        val result = action.callTransform(request).futureValue

        result.userAnswers mustBe answers
      }
    }
  }

  class ActionTestWrapper(answersConnector: AnswersService) extends DataRetrievalActionImpl(answersConnector, mcc) {
    def callTransform[A](request: VerifiedEmailRequest[A]): Future[DataRequest[A]] = transform(request)
  }
}
