/*
 * Copyright 2018 HM Revenue & Customs
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

import models.{FileUploadCount, MRN}
import org.scalacheck.{Arbitrary, Gen}
import wolfendale.scalacheck.regexp.RegexpGen

trait ModelGenerators extends SignedInUserGen {

  implicit val arbitraryMrn: Arbitrary[MRN] =
    Arbitrary(RegexpGen.from(MRN.validRegex).map(MRN(_)).suchThat(_.nonEmpty).map(_.get))

  implicit val arbitraryFileCount: Arbitrary[FileUploadCount] =
    Arbitrary(Gen.chooseNum(1,10).map(FileUploadCount(_).get))
}