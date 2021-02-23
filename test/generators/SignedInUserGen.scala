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

import models.requests.SignedInUser
import org.scalacheck.Gen.listOf
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

trait SignedInUserGen {
  self: Generators =>

  implicit lazy val arbitraryUser: Arbitrary[SignedInUser] = Arbitrary(userGen)

  lazy val userGen: Gen[SignedInUser] = for {
    eori <- eoriString
    enrolments <- enrolmentsGen
  } yield SignedInUser(eori, enrolments)

  val enrolmentGen: Gen[Enrolment] = Gen
    .oneOf(
      "HMRC-CUS-ORG",
      "HMRC-ATED-ORG",
      "IR-CT",
      "IR-SA",
      "HMCE-VATDEC-ORG",
      "HMRC-AWRS-ORG",
      "HMRC-PSA-ORG",
      "IR-SA-AGENT",
      "IR-PAYE",
      "HMRC-EI-ORG"
    )
    .map(Enrolment(_))

  val enrolmentsGen: Gen[Enrolments] =
    listOf(enrolmentGen).map(es => Enrolments(es.toSet))

  case class EORIEnrolment(enrolment: Enrolment)

  val eoriEnrolmentGen: Gen[EORIEnrolment] = {
    eoriString.map { eori =>
      val eoriIdentifier = EnrolmentIdentifier("EORINumber", eori)
      EORIEnrolment(Enrolment("HMRC-CUS-ORG", Seq(eoriIdentifier), ""))
    }
  }
}
