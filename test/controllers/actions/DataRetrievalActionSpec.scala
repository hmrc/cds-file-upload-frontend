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

import base.SpecBase
import connectors.Cache
import controllers.ControllerSpecBase
import generators.SignedInUserGen
import models.requests.{AuthenticatedRequest, EORIRequest, OptionalDataRequest, SignedInUser}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class DataRetrievalActionSpec extends ControllerSpecBase with SignedInUserGen {

  class Harness(dataCacheConnector: Cache) extends DataRetrievalActionImpl(dataCacheConnector, mcc) {
    def callTransform[A](request: EORIRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" when {
    "there is no data in the cache" must {
      "set userAnswers to 'None' in the request" in {

        forAll { (user: SignedInUser, eori: String) =>
          val dataCacheConnector = mock[Cache]
          when(dataCacheConnector.fetch(eqTo(user.internalId))(any[HeaderCarrier])) thenReturn Future.successful(None)
          val action = new Harness(dataCacheConnector)
          val request = EORIRequest(AuthenticatedRequest(fakeRequest, user), eori)

          val futureResult = action.callTransform(request)

          whenReady(futureResult) { result =>
            result.userAnswers.isEmpty mustBe true
          }
        }
      }
    }

    "there is data in the cache" must {
      "build a userAnswers object and add it to the request" in {

        forAll { (user: SignedInUser, eori: String) =>
          val id = user.internalId
          val dataCacheConnector = mock[Cache]
          when(dataCacheConnector.fetch(eqTo(id))(any[HeaderCarrier])) thenReturn Future.successful(Some(new CacheMap(id, Map())))
          val action = new Harness(dataCacheConnector)
          val request = EORIRequest(AuthenticatedRequest(fakeRequest, user), eori)

          val futureResult = action.callTransform(request)

          whenReady(futureResult) { result =>
            result.userAnswers.isDefined mustBe true
          }
        }
      }
    }
  }
}
