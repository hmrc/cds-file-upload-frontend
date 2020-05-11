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

package generators

import models.ContactDetails
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen, Shrink}
import wolfendale.scalacheck.regexp.RegexpGen

trait Generators extends ModelGenerators with JsonGenerators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-.]+$"""

  def intsAboveValue(value: Int): Gen[Int] = Gen.choose(value + 1, Int.MaxValue)

  def nonEmptyString: Gen[String] = arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def saneString: Gen[String] =
    for {
      length <- choose(1, 100)
      chars <- listOfN(length, alphaChar)
    } yield chars.mkString

  def minStringLength(length: Int): Gen[String] =
    for {
      i <- choose(length, length + 500)
      n <- listOfN(i, arbitrary[Char])
    } yield n.mkString

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  private val validEmailGen: Gen[String] = RegexpGen.from("[a-zA-Z0-9\\.!#$%&'*+/=?^_`{|}~-]{1,25}@[a-zA-Z0-9-.]{1,25}")

  private val validPhoneNumberGen: Gen[String] = RegexpGen.from("[\\d\\s\\+()\\-/]{1,15}")

  implicit val arbitraryContactDetails: Arbitrary[ContactDetails] = Arbitrary {
    for {
      name <- nonEmptyString.map(_.take(35))
      companyName <- nonEmptyString.map(_.take(35))
      phoneNumber <- validPhoneNumberGen
      email <- validEmailGen.map(_.take(50))
    } yield ContactDetails(name, companyName, phoneNumber, email)
  }
}
