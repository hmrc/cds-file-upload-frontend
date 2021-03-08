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

package controllers

import java.net.URLEncoder
import base.SpecBase
import config.SecureMessagingConfig
import controllers.actions.{ContactDetailsRequiredAction, DataRetrievalAction, FakeActions}
import models.requests.SignedInUser
import models.{ContactDetails, FileUploadAnswers, SecureMessageAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import services.{FileUploadAnswersService, SecureMessageAnswersService}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, Enrolments}

import scala.concurrent.Future

abstract class ControllerSpecBase extends SpecBase with FakeActions {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockFileUploadAnswersService: FileUploadAnswersService = mock[FileUploadAnswersService]
  val mockSecureMessageAnswersService: SecureMessageAnswersService = mock[SecureMessageAnswersService]
  val secureMessageConfig: SecureMessagingConfig = mock[SecureMessagingConfig]

  def withSecureMessagingEnabled(enabled: Boolean)(test: => Unit): Unit = {
    when(secureMessageConfig.isSecureMessagingEnabled).thenReturn(enabled)

    test
  }

  def withSignedInUser(user: SignedInUser)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), eqTo(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(user.enrolments))

    test
  }

  def withUserWithoutEori(user: SignedInUser)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), eqTo(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(Set())))

    test
  }

  def withAuthError(authException: AuthorisationException)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))

    test
  }

  override protected def beforeEach(): Unit = {
    resetAnswersService()
    reset(secureMessageConfig)
  }

  def resetAnswersService() = {
    reset(mockFileUploadAnswersService)
    when(mockFileUploadAnswersService.upsert(any[FileUploadAnswers])).thenReturn(Future.successful(Some(FileUploadAnswers(""))))
  }

  def theSavedFileUploadAnswers: FileUploadAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[FileUploadAnswers])
    verify(mockFileUploadAnswersService).upsert(captor.capture())
    captor.getValue
  }

  def theSavedSecureMessageAnswers: SecureMessageAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[SecureMessageAnswers])
    verify(mockSecureMessageAnswersService).upsert(captor.capture())
    captor.getValue
  }

  val escaped: String => String = URLEncoder.encode(_, "utf-8")

  def fakeContactDetailsRequiredAction(contactDetails: ContactDetails): ContactDetailsRequiredAction =
    new FakeContactDetailsRequiredAction(contactDetails)

  def fakeDataRetrievalAction(): DataRetrievalAction = new FakeDataRetrievalAction(None)
  def fakeDataRetrievalAction(answers: FileUploadAnswers): DataRetrievalAction = new FakeDataRetrievalAction(Some(answers))
}
