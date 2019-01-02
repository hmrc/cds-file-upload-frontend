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

package generators

import models.{FileUploadCount, MRN, _}
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import wolfendale.scalacheck.regexp.RegexpGen

trait ModelGenerators extends SignedInUserGen {
  self: Generators =>

  implicit val arbitraryMrn: Arbitrary[MRN] =
    Arbitrary(RegexpGen.from(MRN.validRegex).map(MRN(_)).suchThat(_.nonEmpty).map(_.get))

  implicit val arbitraryFileCount: Arbitrary[FileUploadCount] =
    Arbitrary(Gen.chooseNum(1, 10).map(FileUploadCount(_).get))

  val fileUploadRequestGen: Gen[FileUploadRequest] =
    for {
      decId <- stringsWithMaxLength(22)
      size <- intsAboveValue(0)
      files <- Gen.listOf(arbitrary[FileUploadFile]) suchThat (_.nonEmpty)
    } yield {
      FileUploadRequest(decId, size, files).get
    }

  implicit val arbitraryFileUploadRequest: Arbitrary[FileUploadRequest] =
    Arbitrary(fileUploadRequestGen)

  val fileUploadFileGen: Gen[FileUploadFile] =
    for {
      seqNo <- intsAboveValue(0)
      doctype <- arbitrary[String]
    } yield {
      FileUploadFile(seqNo, doctype).get
    }

  implicit val arbitraryFileUploadFile: Arbitrary[FileUploadFile] =
    Arbitrary(fileUploadFileGen)

  implicit val arbitraryFileUploadResponse: Arbitrary[FileUploadResponse] =
    Arbitrary {
      for {
        files <- Gen.listOf(arbitrary[File]) suchThat (_.nonEmpty)
      } yield {
        FileUploadResponse(files)
      }
    }

  implicit val arbitraryFile: Arbitrary[File] =
    Arbitrary {
      for {
        ref <- arbitrary[String].map(_.trim)
        uploadRequest <- arbitrary[UploadRequest]
      } yield {
        File(ref, uploadRequest)
      }
    }

  implicit val arbitraryUploadRequest: Arbitrary[UploadRequest] =
    Arbitrary {
      val tupleGen = saneString.flatMap(a => arbitrary[String].map(b => (a, b.trim)))

      for {
        href <- arbitrary[String].map(_.trim)
        fields <- Gen.listOf(tupleGen).map(_.toMap) suchThat (_.nonEmpty)
      } yield {
        UploadRequest(href, fields)
      }
    }
}