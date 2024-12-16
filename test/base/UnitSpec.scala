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

import config.AppConfig
import generators.Generators
import models.SessionHelper
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testdata.CommonTestData.cacheId
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait UnitSpec extends BaseSpec with Generators with IntegrationPatience with Injector with MessageSpec with ScalaCheckPropertyChecks {

  implicit lazy val appConfig: AppConfig = instanceOf[AppConfig]
  implicit val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  lazy val fakeSessionDataRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "").withSession(SessionHelper.ANSWER_CACHE_ID -> cacheId)
  lazy val fakePostRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "").withMethod("POST")
  lazy val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "").withMethod("GET")

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
