/*
 * Copyright 2019 HM Revenue & Customs
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

package base

import config.AppConfig
import generators.Generators
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with PropertyChecks with Generators with ScalaFutures {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  lazy val fakeRequest = FakeRequest("", "")

  implicit lazy val messages: Messages = messagesApi.preferred(fakeRequest)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  // toJson strips out Some and None and replaces them with string values
  def asFormParams(cc: Product): List[(String, String)] =
    cc.getClass.getDeclaredFields.toList
      .map { f =>
        f.setAccessible(true)
        (f.getName, f.get(cc))
      }
      .flatMap {
        case (n, l: List[_]) if l.headOption.exists(_.isInstanceOf[Product]) =>
          l.zipWithIndex.flatMap {
            case (x, i) => asFormParams(x.asInstanceOf[Product]).map { case (k, v) => (s"$n[$i].$k", v) }
          }
        case (n, Some(p: Product)) => asFormParams(p).map { case (k, v) => (s"$n.$k", v) }
        case (n, Some(a))          => List((n, a.toString))
        case (n, None)             => List((n, ""))
        case (n, a)                => List((n, a.toString))
      }
}
