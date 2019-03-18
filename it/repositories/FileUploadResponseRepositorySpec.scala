package repositories

import models.requests.EORI
import models.{File, FileUploadResponse, UploadRequest}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import suite.FailOnUnindexedQueries

import scala.concurrent.ExecutionContext.Implicits.global

class FileUploadResponseRepositorySpec extends WordSpec with MustMatchers
  with FailOnUnindexedQueries
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with PropertyChecks {

  private lazy val builder: GuiceApplicationBuilder = new GuiceApplicationBuilder()

  "file upload response repository" should {

    "return none" when {

      "get is called on an empty store" in {

        database.map(_.drop()).futureValue

        val app = builder.build()

        running(app) {

          val fileUploadResponseRepo = app.injector.instanceOf[FileUploadResponseRepository]

          val test = for {
            _ <- started(fileUploadResponseRepo)
            result <- fileUploadResponseRepo.getAll(EORI("123"))
          } yield {
            result mustBe List.empty
          }
          test.futureValue
        }
      }
    }

    "get the same values after a put" when {

      "encryption is enabled" in {

        database.map(_.drop()).futureValue

        val app = builder.configure("mongodb.encryption-enabled" -> true).build()

        running(app) {

          val fileUploadResponseRepo = app.injector.instanceOf[FileUploadResponseRepository]

          val testData = List(FileUploadResponse(List(File("reference", UploadRequest("href", Map("123" -> "123"))))))
          val testEORI = EORI("123")

          val test = for {
            _      <- started(fileUploadResponseRepo)
            _      <- fileUploadResponseRepo.put(testEORI, testData)
            result <- fileUploadResponseRepo.getAll(testEORI)
          } yield {
            result mustBe testData
          }

          test.futureValue
        }

      }

      "encryption is disabled" in {

        database.map(_.drop()).futureValue

        val app = builder.configure("mongodb.encryption-enabled" -> false).build()

        running(app) {

          val fileUploadResponseRepo = app.injector.instanceOf[FileUploadResponseRepository]

          val testData = List(FileUploadResponse(List(File("reference", UploadRequest("href", Map("123" -> "123"))))))
          val testEORI = EORI("123")

          val test = for {
            _      <- started(fileUploadResponseRepo)
            _      <- fileUploadResponseRepo.put(testEORI, testData)
            result <- fileUploadResponseRepo.getAll(testEORI)
          } yield {
            result mustBe testData
          }

          test.futureValue
        }
      }
    }
  }
}
