/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

object FakeDataCacheConnector extends Cache {

  def save[A](cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[CacheMap] = Future.successful(cacheMap)

  def fetch(cacheId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = Future.successful(Some(CacheMap(cacheId, Map())))

  def getEntry[A](cacheId: String, key: String)(implicit hc: HeaderCarrier, fmt: Format[A]): Future[Option[A]] = ???
}
