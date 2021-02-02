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

import models.ContactDetails
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen, Shrink}

trait Generators extends ModelGenerators with JsonGenerators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  def intsAboveValue(value: Int): Gen[Int] = Gen.choose(value + 1, Int.MaxValue)

  def alphaString(minLength: Int = 1, maxLength: Int = 35): Gen[String] =
    Gen.choose(minLength, maxLength).flatMap(Gen.listOfN(_, alphaChar)).map(_.mkString)

  def alphaNumString(minLength: Int = 1, maxLength: Int = 35): Gen[String] =
    Gen.choose(minLength, maxLength).flatMap(Gen.listOfN(_, alphaNumChar)).map(_.mkString)

  def numString(minLength: Int = 1, maxLength: Int = 15): Gen[String] =
    Gen.choose(minLength, maxLength).flatMap(Gen.listOfN(_, numChar)).map(_.mkString)

  def stringsWithMaxLength(maxLength: Int): Gen[String] = alphaNumString(1, maxLength)

  def minStringLength(length: Int): Gen[String] = alphaNumString(length, length + 500)

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  def eoriString: Gen[String] =
    for {
      country <- alphaString(2, 2).map(_.mkString)
      code <- numString(10, 10).map(_.mkString)
    } yield s"$country$code"

  def emailString: Gen[String] =
    for {
      name <- alphaNumString(2, 12).map(_.mkString)
      domain <- alphaNumString(2, 12).map(_.mkString)
      suffix <- Gen.oneOf("com", "net", "co.uk")
    } yield s"$name@$domain.$suffix"

  implicit val arbitraryContactDetails: Arbitrary[ContactDetails] = Arbitrary {
    for {
      name <- alphaNumString()
      companyName <- alphaNumString()
      phoneNumber <- numString()
      email <- emailString
    } yield ContactDetails(name, companyName, phoneNumber, email)
  }
}
