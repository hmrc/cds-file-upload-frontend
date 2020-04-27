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

import generators.Generators
import models.requests._
import models.{ContactDetails, UserAnswers}
import org.scalacheck.Arbitrary._
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.stubBodyParser
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

trait FakeActions extends Generators {

  class FakeAuthAction(user: SignedInUser = arbitrary[SignedInUser].sample.get) extends AuthAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
      Future.successful(Right(AuthenticatedRequest(request, user)))
  }

  class FakeEORIAction(eori: String = arbitrary[String].sample.get) extends EORIRequiredAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, EORIRequest[A]]] =
      Future.successful(Right(EORIRequest[A](request, eori)))
  }

  class FakeDataRetrievalAction(cacheMap: Option[CacheMap]) extends DataRetrievalAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def transform[A](request: EORIRequest[A]): Future[OptionalDataRequest[A]] =
      Future.successful(OptionalDataRequest(request, cacheMap.map(UserAnswers(_))))
  }

  class FakeContactDetailsRequiredAction(
    val cacheMap: CacheMap = arbitraryCacheMap.arbitrary.sample.get,
    val contactDetails: ContactDetails = arbitraryContactDetails.arbitrary.sample.get
  ) extends ContactDetailsRequiredAction {
    protected def executionContext = ExecutionContext.global
    def parser = stubBodyParser()
    override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, ContactDetailsRequest[A]]] =
      Future.successful(Right(ContactDetailsRequest(request.request, UserAnswers(cacheMap), contactDetails)))
  }

  class FakeCacheDeleteAction extends CacheDeleteAction {
    override protected def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = Future.successful(None)
    override protected def executionContext: ExecutionContext = ExecutionContext.global
  }
}
