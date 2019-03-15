package repositories

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsString}
import play.api.test.Helpers.running
import suite.FailOnUnindexedQueries
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global

class CacheMapRepositorySpec extends WordSpec with MustMatchers
  with FailOnUnindexedQueries
  with ScalaFutures
  with IntegrationPatience
  with OptionValues {

  private lazy val builder: GuiceApplicationBuilder = new GuiceApplicationBuilder()

  "cache map repository" should {

    "return return none" when {

      "get is called on an empty store" in {

        database.map(_.drop()).futureValue

        val app = builder.build()

        running(app) {

          val cacheMapRepo = app.injector.instanceOf[CacheMapRepository]

          val test = for {
            _      <- started(cacheMapRepo)
            result <- cacheMapRepo.get("12345")
          } yield {
            result mustBe None
          }

          test.futureValue
        }
      }
    }

    "get the same value after a put" when {

      "encryption is enabled" in {

        database.map(_.drop()).futureValue

        val app = builder.configure("mongodb.encryption-enabled" -> true).build()

        running(app) {

          val cacheMapRepo = app.injector.instanceOf[CacheMapRepository]
          val testCache    = CacheMap("12345", Map("key1" -> JsNull, "key2" -> JsString("Hello")))

          val test = for {
            _      <- started(cacheMapRepo)
            _      <- cacheMapRepo.put(testCache)
            result <- cacheMapRepo.get("12345")
          } yield {
            result mustBe Some(testCache)
          }

          test.futureValue
        }
      }

      "encryption is disabled" in {

        database.map(_.drop()).futureValue

        val app = builder.configure("mongodb.encryption-enabled" -> false).build()

        running(app) {

          val cacheMapRepo = app.injector.instanceOf[CacheMapRepository]
          val testCache    = CacheMap("12345", Map("key1" -> JsNull, "key2" -> JsString("Hello")))

          val test = for {
            _      <- started(cacheMapRepo)
            _      <- cacheMapRepo.put(testCache)
            result <- cacheMapRepo.get("12345")
          } yield {
            result mustBe Some(testCache)
          }

          test.futureValue
        }
      }

    }
  }
}