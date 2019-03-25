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

package controllers

import java.net.URLEncoder

import base.SpecBase
import connectors.DataCacheConnector
import controllers.actions.{ContactDetailsRequiredAction, DataRetrievalAction, FakeActions}
import models.ContactDetails
import models.requests.SignedInUser
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ControllerSpecBase extends SpecBase with MockitoSugar with FakeActions with BeforeAndAfterEach {

  lazy val authConnector: AuthConnector = mock[AuthConnector]
  lazy val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  def withSignedInUser(user: SignedInUser)(test: => Unit): Unit = {
    when(
      authConnector
        .authorise(
          any(),
          eqTo(credentials and name and email and affinityGroup and internalId and allEnrolments))(any(), any())
    ).thenReturn(
      Future.successful(new ~(new ~(new ~(new ~(new ~(user.credentials, user.name), user.email), user.affinityGroup), Some(user.internalId)), user.enrolments))
    )

    test
  }

  def withAuthError(authException: AuthorisationException)(test: => Unit): Unit = {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))

    test
  }

  override def beforeEach = {
    reset(dataCacheConnector, authConnector)

    when(dataCacheConnector.save(any()))
      .thenReturn(Future.successful(CacheMap("", Map())))
  }

  val escaped: String => String =
    URLEncoder.encode(_, "utf-8")

  val getEmptyCacheMap: DataRetrievalAction = new FakeDataRetrievalAction(None)

  def getContactDetails(cacheMap: CacheMap, contactDetails: ContactDetails): ContactDetailsRequiredAction =
    new FakeContactDetailsRequiredAction(cacheMap, contactDetails)

  def getCacheMap(cacheMap: CacheMap): DataRetrievalAction = new FakeDataRetrievalAction(Some(cacheMap))
}