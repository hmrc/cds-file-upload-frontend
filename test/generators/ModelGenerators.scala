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

import models.{FileUploadCount, MRN, _}
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.OptionValues
import wolfendale.scalacheck.regexp.RegexpGen

trait ModelGenerators extends SignedInUserGen with OptionValues {
  self: Generators =>

  implicit val arbitraryMrn: Arbitrary[MRN] = Arbitrary(RegexpGen.from(MRN.validRegex).map(MRN(_)).suchThat(_.nonEmpty).map(_.value))

  implicit val arbitraryFileCount: Arbitrary[FileUploadCount] = Arbitrary(Gen.chooseNum(1, 10).map(FileUploadCount(_).value))

  val fileUploadRequestGen: Gen[FileUploadRequest] =
    for {
      decId <- arbitrary[MRN]
      files <- Gen.listOf(arbitrary[FileUploadFile]) suchThat (_.nonEmpty)
    } yield {
      FileUploadRequest(decId, files)
    }

  implicit val arbitraryFileUploadRequest: Arbitrary[FileUploadRequest] =
    Arbitrary(fileUploadRequestGen)

  val fileUploadFileGen: Gen[FileUploadFile] =
    for {
      seqNo   <- intsAboveValue(0)
      doctype <- arbitrary[String]
    } yield {
      FileUploadFile(seqNo, doctype, "").value
    }

  implicit val arbitraryFileUploadFile: Arbitrary[FileUploadFile] = Arbitrary(fileUploadFileGen)

  implicit val arbitraryFileUploadResponse: Arbitrary[FileUploadResponse] =
    Arbitrary {
      for {
        i     <- Gen.choose(1, 10)
        files <- Gen.listOfN(i, arbitrary[FileUpload])
      } yield {
        FileUploadResponse(files)
      }
    }

  implicit val arbitraryWaiting: Arbitrary[Waiting] = Arbitrary(arbitrary[UploadRequest].map(Waiting(_)))

  implicit val arbitraryFileState: Arbitrary[FileState] =
    Arbitrary {
      oneOf(List(arbitrary[Waiting], const(Uploaded), const(Successful), const(Failed), const(VirusDetected), const(UnacceptableMimeType)))
    }

  implicit val arbitraryFile: Arbitrary[FileUpload] =
    Arbitrary {
      for {
        ref       <- saneString
        fileState <- arbitrary[FileState]
        id <- arbitrary[Int]
      } yield {
        FileUpload(ref, fileState, id = id.toString)
      }
    }

  implicit val arbitraryField: Arbitrary[Field] =
    Arbitrary {
      Gen.oneOf(Field.values.toList)
    }

  implicit val arbitraryUploadRequest: Arbitrary[UploadRequest] =
    Arbitrary {
      val tupleGen = arbitrary[Field].flatMap(a => arbitrary[String].map(b => (a.toString, b.trim)))

      for {
        href   <- arbitrary[String].map(_.trim)
        i      <- Gen.choose(1, 10)
        fields <- Gen.listOfN(i, tupleGen).map(_.toMap)
      } yield {
        UploadRequest(href, fields)
      }
    }
}