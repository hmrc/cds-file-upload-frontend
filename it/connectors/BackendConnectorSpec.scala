package connectors

import models._
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import suite.AuthSuite

import scala.concurrent.ExecutionContext.Implicits.global

class BackendConnectorSpec extends WordSpec with MustMatchers
  with AuthSuite
  with IntegrationPatience
  with OptionValues
  with BeforeAndAfterEach {

  private def builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  "backend connector" should {

    "return previously saved data" in {

      val app = builder.build()

      running(app) {

        val connector = app.injector.instanceOf[BackendConnector]
        val eori      = Gen.alphaNumStr.map(_.take(10)).sample.map(EORI).value

        val waiting  = BatchFileUpload(MRN("12GB12345678901234").value, List(File("abc", Waiting(UploadRequest("abc", Map())))))
        val uploaded = BatchFileUpload(MRN("12GB12345678901234").value, List(File("def", Uploaded)))
        val success  = BatchFileUpload(MRN("12GB12345678901234").value, List(File("ghi", Successful)))
        val failed   = BatchFileUpload(MRN("12GB12345678901234").value, List(File("jkl", Failed)))
        val virus    = BatchFileUpload(MRN("12GB12345678901234").value, List(File("mno", VirusDetected)))
        val mimeType = BatchFileUpload(MRN("12GB12345678901234").value, List(File("pqr", UnacceptableMimeType)))

        val test = for {
          hc     <- authenticate(app)
          _      <- connector.save(eori, waiting)(hc)
          _      <- connector.save(eori, uploaded)(hc)
          _      <- connector.save(eori, success)(hc)
          _      <- connector.save(eori, failed)(hc)
          _      <- connector.save(eori, virus)(hc)
          _      <- connector.save(eori, mimeType)(hc)
          result <- connector.fetch(eori)(hc)
        } yield {

          result mustBe List(waiting, uploaded, success, failed, virus, mimeType)
        }

        test.futureValue
      }
    }
  }
}