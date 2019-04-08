package connectors

import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.client.WireMock._
import models.UploadRequest
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.Status
import play.api.libs.Files.TemporaryFile
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

class UpscanS3ConnectorTest extends WordSpec with WiremockTestServer with MustMatchers {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = new UpscanS3Connector()

  private def await[T](future: Future[T]): T = Await.result(future, FiniteDuration(5, TimeUnit.SECONDS))

  "Upload" should {

    "POST to AWS" in {
      stubFor(
        post("/path")
          .willReturn(
            aResponse()
              .withStatus(Status.NO_CONTENT)
          )
      )

      val templateUploading = UploadRequest(
        href = s"$wireMockUrl/path",
        fields = Map(
          "key" -> "value"
        )
      )

      await(connector.upload(templateUploading, TemporaryFile("example-file.json")))

      verify(
        postRequestedFor(urlEqualTo("/path"))
      )
    }

    "Handle Bad Responses" in {
      stubFor(
        post("/path")
          .willReturn(
            aResponse()
              .withStatus(Status.BAD_GATEWAY)
              .withBody("content")
          )
      )

      val templateUploading = UploadRequest(
        href = s"$wireMockUrl/path",
        fields = Map(
          "key" -> "value"
        )
      )

      intercept[RuntimeException] {
        await(connector.upload(templateUploading, TemporaryFile("example-file.json")))
      }.getMessage mustBe "Bad AWS response with status [502] body [content]"

      verify(
        postRequestedFor(urlEqualTo("/path"))
      )
    }

  }

}

