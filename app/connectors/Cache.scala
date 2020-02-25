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

import com.google.inject.Inject
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.CacheMap
import repositories.CacheMapRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoCacheConnector @Inject()(val repository: CacheMapRepository) extends Cache {

  def save[A](cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[CacheMap] = repository.put(cacheMap).map { _ =>
    cacheMap
  }

  def fetch(cacheId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = repository.get(cacheId)

  def getEntry[A](cacheId: String, key: String)(implicit hc: HeaderCarrier, fmt: Format[A]): Future[Option[A]] =
    fetch(cacheId).map(_.flatMap(_.getEntry(key)))
}

trait Cache {

  def save[A](cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[CacheMap]

  def fetch(cacheId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]]

  def getEntry[A](cacheId: String, key: String)(implicit hc: HeaderCarrier, fmt: Format[A]): Future[Option[A]]
}
