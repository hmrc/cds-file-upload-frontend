/*
 * Copyright 2022 HM Revenue & Customs
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
import models.FileUploadAnswers
import models.requests.{AuthenticatedRequest, DataRequest, VerifiedEmailRequest}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import services.FileUploadAnswersService
import testdata.CommonTestData._

import scala.concurrent.Future

class DataRetrievalActionSpec extends ControllerSpecBase {

  private val answersService: FileUploadAnswersService = mock[FileUploadAnswersService]
  private val action: ActionTestWrapper = new ActionTestWrapper(answersService)

  private val answers = FileUploadAnswers(eori)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(answersService)
    when(answersService.findOneOrCreate(eqTo(eori))) thenReturn Future.successful(answers)
  }

  override def afterEach(): Unit = {
    reset(answersService)
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

  class ActionTestWrapper(answersService: FileUploadAnswersService) extends DataRetrievalActionImpl(answersService, mcc) {
    def callTransform[A](request: VerifiedEmailRequest[A]): Future[DataRequest[A]] = transform(request)
  }
}
