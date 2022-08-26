/*
 * Copyright 2022 HM Revenue & Customs
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

package suite

import scala.concurrent.{ExecutionContext, Future}

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

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

  def authenticate(app: Application)(implicit ec: ExecutionContext): Future[Map[String, scala.collection.Seq[String]]] = {
    val ws = app.injector.instanceOf[WSClient]
    ws.url("http://localhost:8585/government-gateway/session/login").post(Json.parse(json)).map(_.headers)
  }
}
