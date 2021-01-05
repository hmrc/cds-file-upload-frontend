/*
 * Copyright 2021 HM Revenue & Customs
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

package generators

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import play.api.libs.json._

trait JsonGenerators {
  self: Generators =>

  implicit lazy val arbitraryJsonValue: Arbitrary[JsValue] =
    Arbitrary {
      Gen.oneOf(arbitraryJsonString.arbitrary, arbitraryJsonNumber.arbitrary, arbitraryJsonBoolean.arbitrary, arbitraryJsonArray.arbitrary)
    }

  implicit val arbitraryJsonString: Arbitrary[JsString] =
    Arbitrary(arbitrary[String].map(JsString))

  implicit val arbitraryJsonNumber: Arbitrary[JsNumber] =
    Arbitrary(arbitrary[BigDecimal].map(JsNumber))

  implicit val arbitraryJsonBoolean: Arbitrary[JsBoolean] =
    Arbitrary(arbitrary[Boolean].map(JsBoolean))

  implicit val arbitraryJsonArray: Arbitrary[JsArray] =
    Arbitrary {
      val valuesGen: Gen[JsValue] = Gen.oneOf(arbitraryJsonString.arbitrary, arbitraryJsonNumber.arbitrary, arbitraryJsonBoolean.arbitrary)

      val f: List[JsValue] => JsArray = in => JsArray(in)
      Gen.listOf(valuesGen).map(f)
    }
}
