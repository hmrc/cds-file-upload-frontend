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

package repositories

import config.AppConfig
import javax.inject.Inject
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class CacheMapRepository @Inject()(cfg: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) extends SessionCache {

  override def baseUri: String = cfg.microservice.services.keystore.baseUri
  override def domain: String = cfg.microservice.services.keystore.domain
  override def defaultSource: String = cfg.microservice.services.keystore.defaultSource

  override def http = httpClient

  def get(id: String)(implicit r: Reads[CacheMap], hc: HeaderCarrier): Future[Option[CacheMap]] =
    fetchAndGetEntry[CacheMap](id)

  def put(cacheMap: CacheMap)(implicit r: Reads[CacheMap], w: Writes[CacheMap], hc: HeaderCarrier): Future[CacheMap] =
    cache[CacheMap](cacheMap.id, cacheMap)
}
