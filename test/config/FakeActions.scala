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

package config

import controllers.{Actions, AuthAction, EORIAction}
import domain.auth.{AuthenticatedRequest, SignedInUser}
import play.api.mvc.{Request, Result}

import scala.concurrent.Future

class FakeActions(signedInUser: SignedInUser) extends Actions(new FakeAuthAction(signedInUser), new FakeEORIAction)

class FakeAuthAction(result: SignedInUser) extends AuthAction(null, null, null)(null) {
  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    Future.successful(Right(AuthenticatedRequest(request, result)))
  }
}

class FakeEORIAction extends EORIAction() {
  override protected def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] =
    Future.successful(None)
}