/*
 * Copyright 2023 HM Revenue & Customs
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

package metrics

import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import javax.inject.Inject
import javax.inject.Singleton
import metrics.MetricIdentifiers._

@Singleton
class SfusMetrics @Inject() (metrics: Metrics) {

  val timers = Map(
    fetchNotificationMetric -> metrics.defaultRegistry.timer(s"$fetchNotificationMetric.timer"),
    fileUploadMetric -> metrics.defaultRegistry.timer(s"$fileUploadMetric.timer"),
    fileUploadRequestMetric -> metrics.defaultRegistry.timer(s"$fileUploadRequestMetric.timer")
  )

  val counters = Map(
    fileUploadMetric -> metrics.defaultRegistry.counter(s"$fileUploadMetric.counter"),
    fileUploadRequestMetric -> metrics.defaultRegistry.counter(s"$fileUploadRequestMetric.counter")
  )

  def startTimer(feature: String): Context = timers(feature).time()

  def incrementCounter(feature: String): Unit = counters(feature).inc()
}

object MetricIdentifiers {
  val fetchNotificationMetric = "notification"
  val fileUploadMetric = "fileUpload"
  val fileUploadRequestMetric = "fileUploadRequest"
}
