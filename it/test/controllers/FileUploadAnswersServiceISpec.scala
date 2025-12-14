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

import base.IntegrationSpec
import models.{FileUploadAnswers, MRN}
import org.mongodb.scala.SingleObservableFuture
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.FileUploadAnswersRepository
import services.FileUploadAnswersService
import testdata.CommonTestData.cacheId

class FileUploadAnswersServiceISpec extends IntegrationSpec {

  private val injector = GuiceApplicationBuilder().injector()

  private val eori = "GB123456789012000"
  private val mrn = MRN("18GB9JLC3CU1LFGVR1")
  private val mrn_2 = MRN("18GB9JLC3CU1LFGVR2")

  private val answers = FileUploadAnswers(eori, cacheId, mrn)
  private val service = injector.instanceOf[FileUploadAnswersService]
  private val repository = injector.instanceOf[FileUploadAnswersRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  private def collectionSize: Int = repository.collection.countDocuments().toFuture().futureValue.toInt

  "FileUploadAnswersService" should {

    "persist a record successfully" when {
      "no record matching eori exists" in {
        service.findOneOrCreate(eori, cacheId).futureValue.eori mustBe answers.eori

        collectionSize mustBe 1
      }
    }

    "retrieve an existing record" when {
      "one exists with the given eori" in {

        repository.insertOne(answers).futureValue.isRight mustBe true
        service.findOne(eori, cacheId).futureValue.value.mrn mustBe mrn
      }
    }

    "replace an existing record" when {
      "one exists with the given eori" in {

        val updatedAnswers = answers.copy(mrn = mrn_2)

        repository.insertOne(answers).futureValue.isRight mustBe true
        service.findOneAndReplace(updatedAnswers).futureValue.mrn mustBe updatedAnswers.mrn
      }
    }

    "remove each record with matching eori" in {
      repository.insertOne(answers).futureValue.isRight mustBe true

      service.remove(eori, cacheId).futureValue

      collectionSize mustBe 0
    }

  }

}
