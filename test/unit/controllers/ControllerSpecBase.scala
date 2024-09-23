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

package controllers

import base.SpecBase
import controllers.actions.{ContactDetailsRequiredAction, DataRetrievalAction, FakeActions}
import models.requests.SignedInUser
import models.{ContactDetails, FileUploadAnswers, SecureMessageAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{mock, reset, verify, when}
import services.{FileUploadAnswersService, SecureMessageAnswersService}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, Enrolments, UnsupportedAffinityGroup}

import java.net.URLEncoder
import scala.concurrent.Future

abstract class ControllerSpecBase extends SpecBase with FakeActions {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockFileUploadAnswersService: FileUploadAnswersService = mock[FileUploadAnswersService]
  val mockSecureMessageAnswersService: SecureMessageAnswersService = mock[SecureMessageAnswersService]

  def withSignedInUser(user: SignedInUser)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and affinityGroup))(any(), any()))
      .thenReturn(Future.successful(new ~(user.enrolments, Some(Individual))))

    test
  }

  def withSignedInAgent()(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and affinityGroup))(any(), any()))
      .thenReturn(Future.failed(UnsupportedAffinityGroup()))

    test
  }

  def withUserWithoutEori(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and affinityGroup))(any(), any()))
      .thenReturn(Future.successful(new ~(Enrolments(Set()), Some(Individual))))

    test
  }

  def withAuthError(authException: AuthorisationException)(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))

    test
  }

  override protected def beforeEach(): Unit =
    when(mockFileUploadAnswersService.findOneAndReplace(any[FileUploadAnswers]))
      .thenReturn(Future.successful(FileUploadAnswers("")))

  override protected def afterEach(): Unit =
    reset(mockAuthConnector, mockFileUploadAnswersService, mockSecureMessageAnswersService)

  protected def resetFileUploadAnswersService(): Unit = {
    reset(mockFileUploadAnswersService)
    when(mockFileUploadAnswersService.findOneAndReplace(any[FileUploadAnswers]))
      .thenReturn(Future.successful(FileUploadAnswers("")))
  }

  def theSavedFileUploadAnswers: FileUploadAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[FileUploadAnswers])
    verify(mockFileUploadAnswersService).findOneAndReplace(captor.capture())
    captor.getValue
  }

  def theSavedSecureMessageAnswers: SecureMessageAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[SecureMessageAnswers])
    verify(mockSecureMessageAnswersService).findOneAndReplace(captor.capture())
    captor.getValue
  }

  val escaped: String => String = URLEncoder.encode(_, "utf-8")

  def fakeContactDetailsRequiredAction(contactDetails: ContactDetails): ContactDetailsRequiredAction =
    new FakeContactDetailsRequiredAction(contactDetails)

  def fakeDataRetrievalAction(): DataRetrievalAction = new FakeDataRetrievalAction(None)
  def fakeDataRetrievalAction(answers: FileUploadAnswers): DataRetrievalAction = new FakeDataRetrievalAction(Some(answers))
}
