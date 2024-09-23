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

package base

import base.UnitViewSpec.realMessagesApi
import com.codahale.metrics.SharedMetricRegistries
import config.AppConfig
import generators.Generators
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait SpecBase
    extends PlaySpec with BeforeAndAfterEach with ScalaCheckPropertyChecks with Generators with ScalaFutures with IntegrationPatience with Injector {

  SharedMetricRegistries.clear()

  implicit lazy val appConfig: AppConfig = instanceOf[AppConfig]
  implicit val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  lazy val fakeRequest = FakeRequest("", "")
  lazy val fakePostRequest = FakeRequest("", "").withMethod("POST")
  lazy val fakeGetRequest = FakeRequest("", "").withMethod("GET")

  implicit lazy val messages: Messages =
    new AllMessageKeysAreMandatoryMessages(SpecBase.messagesApi.preferred(fakeRequest))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  // toJson strips out Some and None and replaces them with string values
  def asFormParams(cc: Product): List[(String, String)] =
    cc.getClass.getDeclaredFields.toList.map { f =>
      f.setAccessible(true)
      (f.getName, f.get(cc))
    }.flatMap {
      case (n, l: List[_]) if l.headOption.exists(_.isInstanceOf[Product]) =>
        l.zipWithIndex.flatMap { case (x, i) =>
          asFormParams(x.asInstanceOf[Product]).map { case (k, v) => (s"$n[$i].$k", v) }
        }
      case (n, Some(p: Product)) => asFormParams(p).map { case (k, v) => (s"$n.$k", v) }
      case (n, Some(a))          => List((n, a.toString))
      case (n, None)             => List((n, ""))
      case (n, a)                => List((n, a.toString))
    }
}

object SpecBase extends Injector {
  val messagesApi: MessagesApi = instanceOf[MessagesApi]
}

private class AllMessageKeysAreMandatoryMessages(msg: Messages) extends Messages {

  override def asJava: play.i18n.Messages = new play.i18n.MessagesImpl(lang.asJava, realMessagesApi.asJava)

  override def messages: Messages = msg.messages

  override def lang: Lang = msg.lang

  override def apply(key: String, args: Any*): String =
    if (msg.isDefinedAt(key))
      msg.apply(key, args: _*)
    else throw new AssertionError(s"Message Key is not configured for {$key}")

  override def apply(keys: Seq[String], args: Any*): String =
    if (keys.exists(key => !msg.isDefinedAt(key)))
      msg.apply(keys, args)
    else throw new AssertionError(s"Message Key is not configured for {$keys}")

  override def translate(key: String, args: Seq[Any]): Option[String] = msg.translate(key, args)

  override def isDefinedAt(key: String): Boolean = msg.isDefinedAt(key)
}
