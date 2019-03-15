package repositories

import models.requests.EORI
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsString}
import play.api.test.Helpers.running
import suite.FailOnUnindexedQueries
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global

class FileUploadResponseRepositorySpec extends WordSpec with MustMatchers
  with FailOnUnindexedQueries
  with ScalaFutures
  with IntegrationPatience
  with OptionValues {

  private lazy val builder: GuiceApplicationBuilder = new GuiceApplicationBuilder()

  "file upload response repository" should {

    "return none" when {

      "get is called on an empty store" in {

        database.map(_.drop()).futureValue

        val app = builder.build()

        running(app) {

          val fileUploadResponseRepo = app.injector.instanceOf[FileUploadResponseRepository]

          val test = for {
            _      <- started(fileUploadResponseRepo)
            result <- fileUploadResponseRepo.getAll(EORI("123"))
          } yield {
            result mustBe List.empty
          }
          test.futureValue
        }
      }
    }
  }
}
