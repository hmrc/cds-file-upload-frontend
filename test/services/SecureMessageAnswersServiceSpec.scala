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

package services

import base.UnitSpec
import models.{ExportMessages, SecureMessageAnswers}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{mock, reset, when}
import repositories.SecureMessageAnswersRepository
import services.SecureMessageAnswersService

import scala.concurrent.Future

class SecureMessageAnswersServiceSpec extends UnitSpec {

  private val eori = "eori"
  private val secureMessageAnswers = SecureMessageAnswers(eori, ExportMessages)

  private val mockRepository = mock[SecureMessageAnswersRepository]

  private val service = new SecureMessageAnswersService(mockRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository)
  }

  "SecureMessageAnswersService" should {
    "return the expected 'SecureMessageAnswers' instance" when {

      "the 'findOne' method is called" in {
        when(mockRepository.findOne(any[String], any[String])).thenReturn(Future.successful(Some(secureMessageAnswers)))
        service.findOne(eori, any[String]).futureValue.value mustBe secureMessageAnswers
      }

      "the 'findOneAndReplace' method is called" in {
        when(mockRepository.findOneAndReplace(any[SecureMessageAnswers])).thenReturn(Future.successful(secureMessageAnswers))
        service.findOneAndReplace(secureMessageAnswers).futureValue mustBe secureMessageAnswers
      }
    }
  }
}
