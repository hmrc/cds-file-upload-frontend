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

import base.UnitSpec
import config.SecureMessagingConfig
import models.exceptions.InvalidFeatureStateException
import models.requests.VerifiedEmailRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SecureMessagingFeatureActionSpec extends UnitSpec with BeforeAndAfterEach with ScalaFutures {

  private val secureMessagingConfig = mock[SecureMessagingConfig]
  private val request = mock[VerifiedEmailRequest[_]]
  private val functionBlock = mock[VerifiedEmailRequest[_] => Future[Result]]
  private val controllerResult = mock[Result]

  private val secureMessagingFeatureAction = new SecureMessagingFeatureAction(secureMessagingConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(secureMessagingConfig, request, functionBlock)
    when(functionBlock.apply(any())).thenReturn(Future.successful(controllerResult))
  }

  override def afterEach(): Unit = {
    reset(secureMessagingConfig, request, functionBlock)

    super.afterEach()
  }

  "SecureMessagingFeatureAction on invokeBlock" when {

    "SecureMessagingFeature is enabled" should {

      "call provided function block" in {

        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

        secureMessagingFeatureAction.invokeBlock(request, functionBlock).futureValue

        verify(functionBlock).apply(any())
      }

      "pass provided request to function block" in {

        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

        secureMessagingFeatureAction.invokeBlock(request, functionBlock).futureValue

        verify(functionBlock).apply(eqTo(request))
      }
    }

    "SecureMessagingFeature is disabled" should {

      "throw InvalidFeatureStateException" in {

        when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(false)

        an[InvalidFeatureStateException] mustBe thrownBy {
          secureMessagingFeatureAction.invokeBlock(request, functionBlock).futureValue
        }
      }
    }
  }

}
