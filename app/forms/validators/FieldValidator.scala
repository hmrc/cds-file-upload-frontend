/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.validators

object FieldValidator {

  implicit class PredicateOpsForBooleans[A](first: Boolean) {
    def and(second: A => Boolean): A => Boolean =
      (arg: A) => first && second(arg)

    def and(second: Boolean): Boolean = first && second

    def or(second: A => Boolean): A => Boolean =
      (arg: A) => first || second(arg)

    def or(second: Boolean): Boolean = first || second
  }

  private val allowedSpecialChars = Set(',', '.', '-', '\'', '/', ' ')

  val isEmpty: String => Boolean = (input: String) => input.isEmpty

  val nonEmpty: String => Boolean = (input: String) => input.nonEmpty

  val noLongerThan: Int => String => Boolean = (length: Int) => (input: String) => input.length <= length

  val noShorterThan: Int => String => Boolean = (length: Int) => (input: String) => input.length >= length

  val hasSpecificLength: Int => String => Boolean = (length: Int) => (input: String) => input.length == length

  val lengthInRange: Int => Int => String => Boolean = (min: Int) => (max: Int) => (input: String) => input.length >= min && input.length <= max

  val isNumeric: String => Boolean = (input: String) => input.forall(_.isDigit)

  val isAllCapitalLetter: String => Boolean = (input: String) => input.forall(_.isUpper)

  val isAlphabetic: String => Boolean = (input: String) => input.forall(_.isLetter)

  val isAlphanumeric: String => Boolean = (input: String) => input.forall(_.isLetterOrDigit)

  val isAlphanumericWithSpecialCharacters: Set[Char] => String => Boolean =
    (allowedChars: Set[Char]) => (input: String) => input.filter(!_.isLetterOrDigit).forall(allowedChars)

  val isAlphanumericWithAllowedSpecialCharacters: String => Boolean =
    (input: String) => input.filter(!_.isLetterOrDigit).forall(allowedSpecialChars)

  val startsWithCapitalLetter: String => Boolean = (input: String) => input.headOption.exists(_.isUpper)

  val isContainedIn: Iterable[String] => String => Boolean =
    (iterable: Iterable[String]) => (input: String) => iterable.exists(_ == input)

  val containsDuplicates: Iterable[_] => Boolean = (input: Iterable[_]) => input.toSet.size != input.size

  val areAllElementsUnique: Iterable[_] => Boolean = (input: Iterable[_]) => input.toSet.size == input.size

}
