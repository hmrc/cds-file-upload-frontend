/*
 * Copyright 2018 HM Revenue & Customs
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

import base.SpecBase
import domain.auth.SignedInUser
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

class ControllerSpecBase extends SpecBase with MockitoSugar {

  val authConnector: AuthConnector = mock[AuthConnector]

  def withSignedInUser(user: SignedInUser)(test: => Unit): Unit = {
    when(
      authConnector
        .authorise(
          any(),
          eqTo(credentials and name and email and affinityGroup and internalId and allEnrolments))(any(), any())
    ).thenReturn(
      Future.successful(new ~(new ~(new ~(new ~(new ~(user.credentials, user.name), user.email), user.affinityGroup), user.internalId), user.enrolments))
    )

    test
  }

  def withAuthError(authException: AuthorisationException)(test: => Unit): Unit = {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))

    test
  }

  val escaped: String => String = play.utils.UriEncoding.encodePathSegment(_, "utf-8")
}