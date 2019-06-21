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

import config.{AppConfig, Assets, ContactFrontend}
import generators.Generators
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.{Configuration, Environment}
import play.api.http.{DefaultFileMimeTypes, FileMimeTypesConfiguration}
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, Messages, MessagesApi}
import play.api.mvc.{DefaultActionBuilder, DefaultMessagesActionBuilderImpl, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.{FakeRequest, NoMaterializer}
import play.api.test.Helpers.{stubBodyParser, stubLangs, stubPlayBodyParsers}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase extends PlaySpec with MockitoSugar with BeforeAndAfterEach with PropertyChecks with Generators with ScalaFutures with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]


    //TODO remove GuiceOneServerPerSuite to speed up the tests
  //Gabor
//  val env = Environment.simple()
//  val configuration = Configuration.load(env)
//  //implicit val messagesApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))
//  implicit val messagesApi = new DefaultMessagesApi(Map("en" ->
//    Map("error.min" -> "minimum!")
//  ))
//  implicit val appConfig = pureconfig.loadConfigOrThrow[AppConfig]

  lazy val fakeRequest = FakeRequest("", "")

  implicit lazy val messages: Messages = messagesApi.preferred(fakeRequest)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val executionContext = ExecutionContext.global
//Gabor
  //  implicit val mcc: MessagesControllerComponents =  DefaultMessagesControllerComponents(
//    messagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser(),  messagesApi)(executionContext),
//    actionBuilder = DefaultActionBuilder(stubBodyParser())(ExecutionContext.global),
//    parsers = stubPlayBodyParsers(NoMaterializer),
//    messagesApi = messagesApi,
//    langs = stubLangs(),
//    fileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration(Map.empty)),
//    executionContext = executionContext
//  )


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
