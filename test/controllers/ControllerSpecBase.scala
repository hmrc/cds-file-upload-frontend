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

package controllers

import java.net.URLEncoder

import base.SpecBase
import connectors.Cache
import controllers.actions.{ContactDetailsRequiredAction, DataRetrievalAction, FakeActions}
import models.ContactDetails
import models.requests.SignedInUser
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

abstract class ControllerSpecBase extends SpecBase with FakeActions {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheConnector: Cache = mock[Cache]

  def withSignedInUser(user: SignedInUser)(test: => Unit): Unit = {
    when(
      mockAuthConnector
        .authorise(any(), eqTo(credentials and name and email and affinityGroup and internalId and allEnrolments))(any(), any())
    ).thenReturn(
      Future.successful(
        new ~(
          new ~(new ~(new ~(new ~(Some(user.credentials), Some(user.name)), user.email), user.affinityGroup), Some(user.internalId)),
          user.enrolments
        )
      )
    )

    test
  }

  def withAuthError(authException: AuthorisationException)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))

    test
  }

  override protected def beforeEach(): Unit = {
    reset(mockDataCacheConnector, mockAuthConnector)
    when(mockDataCacheConnector.save(any[CacheMap])(any[HeaderCarrier])).thenReturn(Future.successful(CacheMap("", Map())))
  }

  val escaped: String => String = URLEncoder.encode(_, "utf-8")

  def fakeContactDetailsRequiredAction(cacheMap: CacheMap, contactDetails: ContactDetails): ContactDetailsRequiredAction =
    new FakeContactDetailsRequiredAction(cacheMap, contactDetails)

  def fakeDataRetrievalAction(cacheMap: CacheMap): DataRetrievalAction = new FakeDataRetrievalAction(Some(cacheMap))
}
