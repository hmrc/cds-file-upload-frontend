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

import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

trait IntegrationSpec extends BaseSpec with GuiceOneServerPerSuite with WsScalaTestClient {

  val disableMetricsConfiguration: Configuration = Configuration.from(Map("metrics.jvm" -> "false", "metrics.logback" -> "false"))

  val databaseName = "test-cds-file-upload-frontend"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.codahale.metrics.MetricRegistry]
      .configure(disableMetricsConfiguration)
      .configure(Map("mongodb.uri" -> s"mongodb://localhost:27017/$databaseName"))
      .build()
}
