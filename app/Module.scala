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

import com.google.inject.{AbstractModule, Provides}
import config.AppConfig
import connectors.{CustomsDeclarationsConnector, CustomsDeclarationsConnectorImpl, DataCacheConnector, MongoCacheConnector}
import controllers.actions._
import javax.inject.Singleton
import services.{CustomsDeclarationsService, CustomsDeclarationsServiceImpl}

class Module extends AbstractModule {

  val cfg = pureconfig.loadConfigOrThrow[AppConfig]

  override def configure(): Unit = {
    // Bind the actions for DI
    bind(classOf[AuthAction]).to(classOf[AuthActionImpl]).asEagerSingleton()
    bind(classOf[EORIAction]).to(classOf[EORIActionImpl]).asEagerSingleton()
    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[MrnRequiredAction]).to(classOf[MrnRequiredActionImpl]).asEagerSingleton()
    bind(classOf[FileUploadResponseRequiredAction]).to(classOf[FileUploadResponseRequiredActionImpl]).asEagerSingleton()
    bind(classOf[CustomsDeclarationsConnector]).to(classOf[CustomsDeclarationsConnectorImpl]).asEagerSingleton()
    bind(classOf[DataCacheConnector]).to(classOf[MongoCacheConnector]).asEagerSingleton()
    bind(classOf[CustomsDeclarationsService]).to(classOf[CustomsDeclarationsServiceImpl]).asEagerSingleton()
  }

  @Provides @Singleton
  def appConfig: AppConfig = cfg
}
