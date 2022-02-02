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

package base

import com.codahale.metrics.{Counter, Timer}
import metrics.SfusMetrics
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

trait SfusMetricsMock extends AnyWordSpec with MockitoSugar with BeforeAndAfterEach {

  val sfusMetrics = mock[SfusMetrics]

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(sfusMetrics.counters).thenReturn(Map.empty[String, Counter])
    when(sfusMetrics.timers).thenReturn(Map.empty[String, Timer])
    when(sfusMetrics.startTimer(any())).thenReturn(mock[Timer.Context])
    doNothing().when(sfusMetrics).incrementCounter(any())
  }

  override protected def afterEach(): Unit = {
    reset(sfusMetrics)

    super.afterEach()
  }
}
