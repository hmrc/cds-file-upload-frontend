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

package connectors

import base.SpecBase
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.JsString
import repositories.CacheMapRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class MongoCacheConnectorSpec extends SpecBase {

  ".save" must {

    "save the cache map to the Mongo repository" in {

      val mockCacheMapRepository = mock[CacheMapRepository]
      val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

      forAll(arbitrary[CacheMap]) { cacheMap =>
        when(mockCacheMapRepository.put(eqTo(cacheMap))(any(), any(), any[HeaderCarrier])) thenReturn Future.successful(cacheMap)
        mongoCacheConnector.save(cacheMap).futureValue mustBe cacheMap
      }
    }
  }

  ".fetch" when {

    "there isn't a record for this key in Mongo" must {

      "return None" in {

        val mockCacheMapRepository = mock[CacheMapRepository]
        when(mockCacheMapRepository.get(eqTo("non-existent-cacheId"))(any(), any[HeaderCarrier])) thenReturn Future.successful(None)
        val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

        mongoCacheConnector.fetch("non-existent-cacheId").futureValue mustBe empty
      }
    }

    "a record exists for this key" must {

      "return the record" in {

        val mockCacheMapRepository = mock[CacheMapRepository]

        val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

        forAll(arbitrary[CacheMap]) { cacheMap =>
          when(mockCacheMapRepository.get(eqTo(cacheMap.id))(any(), any[HeaderCarrier])) thenReturn Future.successful(Some(cacheMap))
          mongoCacheConnector.fetch(cacheMap.id).futureValue mustBe Some(cacheMap)
        }
      }
    }
  }

  ".getEntry" when {

    "there isn't a record for this key in Mongo" must {

      "return None" in {

        val mockCacheMapRepository = mock[CacheMapRepository]
        val cacheId = "not-found"

        when(mockCacheMapRepository.get(eqTo(cacheId))(any(), any[HeaderCarrier])) thenReturn Future.successful(None)

        val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

        mongoCacheConnector.getEntry[String](cacheId, "some key").futureValue mustBe empty
      }
    }

    "a record exists in Mongo but this key is not present" must {

      "return None" in {

        val mockCacheMapRepository = mock[CacheMapRepository]

        val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

        val gen = for {
          key <- nonEmptyString
          cacheMap <- arbitrary[CacheMap]
        } yield (key, cacheMap copy (data = cacheMap.data - key))

        forAll(gen) {
          case (key, cacheMap) =>
            when(mockCacheMapRepository.get(eqTo(cacheMap.id))(any(), any[HeaderCarrier])) thenReturn Future.successful(Some(cacheMap))
            mongoCacheConnector.getEntry[String](cacheMap.id, key).futureValue mustBe empty
        }
      }
    }

    "a record exists in Mongo with this key" must {

      "return the key's value" in {

        val mockCacheMapRepository = mock[CacheMapRepository]

        val mongoCacheConnector = new MongoCacheConnector(mockCacheMapRepository)

        val gen = for {
          key <- nonEmptyString
          value <- nonEmptyString
          cacheMap <- arbitrary[CacheMap]
        } yield (key, value, cacheMap copy (data = cacheMap.data + (key -> JsString(value))))

        forAll(gen) {
          case (key, value, cacheMap) =>
            when(mockCacheMapRepository.get(eqTo(cacheMap.id))(any(), any[HeaderCarrier])) thenReturn Future.successful(Some(cacheMap))
            mongoCacheConnector.getEntry[String](cacheMap.id, key).futureValue mustBe Some(value)
        }
      }
    }
  }
}
