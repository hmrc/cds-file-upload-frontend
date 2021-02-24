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

import config.SecureMessagingConfig

import javax.inject.{Inject, Singleton}

import models.exceptions.InvalidFeatureStateException
import models.requests.VerifiedEmailRequest
import play.api.mvc.{ActionFunction, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SecureMessagingFeatureAction @Inject()(secureMessagingConfig: SecureMessagingConfig)(implicit ec: ExecutionContext)
    extends ActionFunction[VerifiedEmailRequest, VerifiedEmailRequest] {

  override def invokeBlock[A](request: VerifiedEmailRequest[A], block: VerifiedEmailRequest[A] => Future[Result]): Future[Result] =
    if (secureMessagingConfig.isSecureMessagingEnabled) block(request) else throw InvalidFeatureStateException

  override protected def executionContext: ExecutionContext = ec
}
