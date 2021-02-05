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

import generators.Generators
import models.requests._
import models.{ContactDetails, MRN, UserAnswers}
import org.scalacheck.Arbitrary._
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.stubBodyParser

import scala.concurrent.{ExecutionContext, Future}

trait FakeActions extends Generators {

  class FakeAuthAction(user: SignedInUser = arbitrary[SignedInUser].sample.get) extends AuthAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
      Future.successful(Right(AuthenticatedRequest(request, user)))
  }

  class FakeEORIAction(eori: String = eoriString.sample.get) extends EORIRequiredAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, EORIRequest[A]]] =
      Future.successful(Right(EORIRequest[A](request, eori)))
  }

  class FakeDataRetrievalAction(answers: Option[UserAnswers] = None) extends DataRetrievalAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def transform[A](request: VerifiedEmailRequest[A]): Future[DataRequest[A]] =
      Future.successful(DataRequest(request, answers.getOrElse(UserAnswers(request.eori))))
  }

  class FakeContactDetailsRequiredAction(val contactDetails: ContactDetails = arbitraryContactDetails.arbitrary.sample.get)
      extends ContactDetailsRequiredAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: MrnRequest[A]): Future[Either[Result, ContactDetailsRequest[A]]] =
      Future.successful(Right(ContactDetailsRequest(request, request.userAnswers, contactDetails)))
  }

  class FakeMrnRequiredAction(val mrn: MRN = arbitraryMrn.arbitrary.sample.get) extends MrnRequiredAction {
    protected def executionContext = ExecutionContext.global
    override protected def refine[A](request: DataRequest[A]): Future[Either[Result, MrnRequest[A]]] =
      Future.successful(Right(MrnRequest(request.request.request, request.userAnswers, mrn)))
  }

  class FakeVerifiedEmailAction(email: String = emailString.sample.get) extends VerifiedEmailAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: EORIRequest[A]): Future[Either[Result, VerifiedEmailRequest[A]]] =
      Future.successful(Right(VerifiedEmailRequest[A](request, email)))
  }
}
