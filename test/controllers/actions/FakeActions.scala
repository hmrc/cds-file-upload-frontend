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

package controllers.actions

import generators.Generators
import models._
import models.requests._
import org.scalacheck.Arbitrary._
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.stubBodyParser
import testdata.CommonTestData.cacheId

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

trait FakeActions extends Generators {

  class FakeAuthAction(user: SignedInUser = arbitrary[SignedInUser].sample.get) extends AuthAction {
    protected def executionContext = global
    def parser = stubBodyParser()
    override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
      Future.successful(Right(AuthenticatedRequest(request, user)))
  }

  class FakeDataRetrievalAction(answers: Option[FileUploadAnswers] = None) extends DataRetrievalAction {
    protected def executionContext = global
    def parser = stubBodyParser()
    override protected def transform[A](request: VerifiedEmailRequest[A]): Future[DataRequest[A]] =
      Future.successful(DataRequest(request, answers.getOrElse(FileUploadAnswers(request.eori, cacheId))))
  }

  class FakeContactDetailsRequiredAction(val contactDetails: ContactDetails = arbitraryContactDetails.arbitrary.sample.get)
      extends ContactDetailsRequiredAction {
    protected def executionContext = global
    def parser = stubBodyParser()
    override protected def refine[A](request: MrnRequest[A]): Future[Either[Result, ContactDetailsRequest[A]]] =
      Future.successful(Right(ContactDetailsRequest(request, request.userAnswers, contactDetails)))
  }

  class FakeMrnRequiredAction(val mrn: MRN = arbitraryMrn.arbitrary.sample.get) extends MrnRequiredAction {
    protected def executionContext = global
    override protected def refine[A](request: DataRequest[A]): Future[Either[Result, MrnRequest[A]]] =
      Future.successful(Right(MrnRequest(request.request.request, request.userAnswers, mrn)))
  }

  class FakeVerifiedEmailAction(email: String = emailString.sample.get) extends VerifiedEmailAction {
    protected def executionContext = global
    def parser = stubBodyParser()
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, VerifiedEmailRequest[A]]] =
      Future.successful(Right(VerifiedEmailRequest[A](request, email)))
  }

  class FakeMessageFilterAction(eori: String = eoriString.sample.get, tag: MessageFilterTag = ExportMessages) extends MessageFilterAction {
    protected def executionContext = global
    def parser = stubBodyParser()
    override protected def refine[A](request: VerifiedEmailRequest[A]): Future[Either[Result, MessageFilterRequest[A]]] =
      Future.successful(Right(MessageFilterRequest[A](request, SecureMessageAnswers(eori, tag, cacheId))))
  }
}

object FakeActions extends FakeActions
