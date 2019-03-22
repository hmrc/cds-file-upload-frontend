package suite

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Headers
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, Token}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait AuthSuite extends ScalaFutures {

  case class GatewayToken(gatewayToken: String)

  object GatewayToken {

    implicit val formats = Json.format[GatewayToken]
  }

  val json = """{
               | "credId": "a-cred-id",
               |  "affinityGroup": "Organisation",
               |  "credentialStrength": "strong",
               |  "enrolments": [
               |    {
               |      "key": "HMRC-CUS-ORG",
               |      "identifiers": [
               |        {
               |          "key": "EORINumber",
               |          "value": "ZZ123456789000"
               |        }
               |      ],
               |      "state": "Activated"
               |    }
               |  ]
               |}""".stripMargin

  def authenticate(app: Application)(implicit ec: ExecutionContext): Future[HeaderCarrier] = {

    val ws = app.injector.instanceOf[WSClient]

    ws.url("http://localhost:8585/government-gateway/session/login").post(Json.parse(json)).map { auth =>

      val headers = Headers(auth.allHeaders.mapValues(_.headOption.getOrElse("")).toList: _*)
      HeaderCarrierConverter.fromHeadersAndSession(headers)
    }
  }
}