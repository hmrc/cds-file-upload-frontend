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

import com.google.inject.{AbstractModule, Provides}
import config.AppConfig
import controllers.actions._
import play.filters.csrf.CSRFConfig
import pureconfig.generic.auto._

import javax.inject.Singleton

class TestModule(configOverride: AppConfig => AppConfig) extends AbstractModule {

  import pureconfig.ConfigSource

  val cfg: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]

  val csrfConfig: CSRFConfig = CSRFConfig(shouldProtect = !_.uri.matches("*test-only*"))

  override def configure(): Unit = {
    // Bind the actions for DI
    bind(classOf[AuthAction]).to(classOf[AuthActionImpl]).asEagerSingleton()
    bind(classOf[VerifiedEmailAction]).to(classOf[VerifiedEmailActionImpl]).asEagerSingleton()
    bind(classOf[ContactDetailsRequiredAction]).to(classOf[ContactDetailsRequiredActionImpl]).asEagerSingleton()
    bind(classOf[MrnRequiredAction]).to(classOf[MrnRequiredActionImpl]).asEagerSingleton()
    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[MessageFilterAction]).to(classOf[MessageFilterActionImpl]).asEagerSingleton()
  }

  @Provides @Singleton
  def appConfig: AppConfig = configOverride(cfg)
}
