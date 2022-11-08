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

package filters

import akka.stream.Materializer
import com.typesafe.config.ConfigException
import generators.Generators
import org.mockito.MockitoSugar.mock
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Configuration
import play.api.mvc.Call

class AllowListFilterSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators {

  val mockMaterializer = mock[Materializer]

  val otherConfigGen = Gen.mapOf[String, String](for {
    key <- Gen.alphaNumStr suchThat (_.nonEmpty)
    value <- alphaNumString()
  } yield (key, value))

  "the list of allowListed IP addresses" - {

    "must throw an exception" - {

      "when the underlying config value is not there" in {

        forAll(otherConfigGen, alphaNumString(), alphaNumString()) { (otherConfig, destination, excluded) =>
          whenever(!otherConfig.contains("filters.allowList.ips")) {

            val config = Configuration(
              (otherConfig +
                ("filters.allowList.destination" -> destination) +
                ("filters.allowList.excluded" -> excluded)).toSeq: _*
            )

            assertThrows[ConfigException] {
              new AllowListFilter(config, mockMaterializer)
            }
          }
        }
      }
    }

    "must be empty" - {

      "when the underlying config value is empty" in {

        forAll(otherConfigGen, alphaNumString(), alphaNumString()) { (otherConfig, destination, excluded) =>
          val config = Configuration(
            (otherConfig +
              ("filters.allowList.destination" -> destination) +
              ("filters.allowList.excluded" -> excluded) +
              ("filters.allowList.ips" -> "")).toSeq: _*
          )

          val allowListFilter = new AllowListFilter(config, mockMaterializer)

          allowListFilter.allowlist mustBe empty
        }
      }
    }

    "must contain all of the values" - {

      "when given a comma-separated list of values" in {

        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, alphaNumString(), alphaNumString()) { (ips, otherConfig, destination, excluded) =>
          val ipString = ips.mkString(",")

          val config = Configuration(
            (otherConfig +
              ("filters.allowList.destination" -> destination) +
              ("filters.allowList.excluded" -> excluded) +
              ("filters.allowList.ips" -> ipString)).toSeq: _*
          )

          val allowListFilter = new AllowListFilter(config, mockMaterializer)

          allowListFilter.allowlist must contain theSameElementsAs ips
        }
      }
    }
  }

  "the destination for non-allowListed visitors" - {

    "must throw an exception" - {

      "when the underlying config value is not there" in {

        forAll(otherConfigGen, alphaNumString(), alphaNumString()) { (otherConfig, destination, excluded) =>
          whenever(!otherConfig.contains("filters.allowList.destination")) {

            val config = Configuration(
              (otherConfig +
                ("filters.allowList.ips" -> destination) +
                ("filters.allowList.excluded" -> excluded)).toSeq: _*
            )

            assertThrows[ConfigException] {
              new AllowListFilter(config, mockMaterializer)
            }
          }
        }
      }
    }

    "must return a Call to the destination" in {

      forAll(otherConfigGen, alphaNumString(), alphaNumString(), alphaNumString()) { (otherConfig, ips, destination, excluded) =>
        val config = Configuration(
          (otherConfig +
            ("filters.allowList.ips" -> destination) +
            ("filters.allowList.excluded" -> excluded) +
            ("filters.allowList.destination" -> destination)).toSeq: _*
        )

        val allowListFilter = new AllowListFilter(config, mockMaterializer)

        allowListFilter.destination mustEqual Call("GET", destination)
      }
    }
  }

  "the list of excluded paths" - {

    "must throw an exception" - {

      "when the underlying config value is not there" in {

        forAll(otherConfigGen, alphaNumString(), alphaNumString()) { (otherConfig, destination, excluded) =>
          whenever(!otherConfig.contains("filters.allowList.excluded")) {

            val config = Configuration(
              (otherConfig +
                ("filters.allowList.destination" -> destination) +
                ("filters.allowList.ips" -> excluded)).toSeq: _*
            )

            assertThrows[ConfigException] {
              new AllowListFilter(config, mockMaterializer)
            }
          }
        }
      }
    }

    "must return Calls to all of the values" - {

      "when given a comma-separated list of values" in {

        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, alphaNumString(), alphaNumString()) { (excludedPaths, otherConfig, destination, ips) =>
          val excludedPathString = excludedPaths.mkString(",")

          val config = Configuration(
            (otherConfig +
              ("filters.allowList.destination" -> destination) +
              ("filters.allowList.excluded" -> excludedPathString) +
              ("filters.allowList.ips" -> ips)).toSeq: _*
          )

          val expectedCalls = excludedPaths.map(Call("GET", _))

          val allowListFilter = new AllowListFilter(config, mockMaterializer)

          allowListFilter.excludedPaths must contain theSameElementsAs expectedCalls
        }
      }
    }
  }
}
