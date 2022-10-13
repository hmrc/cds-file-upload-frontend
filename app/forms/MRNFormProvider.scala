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

package forms

import models.MRN
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

class MRNFormProvider {

  def apply(): Form[MRN] =
    Form("value" -> of(mrnFormatter()))

  private def isMissing(key: String, data: Map[String, String]) =
    data.get(key).map(_.trim) match {
      case None | Some("") => Left(Seq(FormError(key, "mrn.missing")))
      case Some(mrn)       => Right(mrn)
    }

  private def isInvalid(key: String, data: String) =
    MRN(data) match {
      case None      => Left(Seq(FormError(key, "mrn.invalid")))
      case Some(mrn) => Right(mrn)
    }

  def mrnFormatter(): Formatter[MRN] = new Formatter[MRN] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], MRN] =
      for {
        value <- isMissing(key, data)
        mrn <- isInvalid(key, value.toUpperCase)
      } yield mrn

    override def unbind(key: String, value: MRN): Map[String, String] =
      Map(key -> value.value)
  }
}
