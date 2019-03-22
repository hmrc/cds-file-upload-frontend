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

package models

import play.api.libs.json.Json
import play.json.extra.Variants

sealed trait FileState
final case class Waiting(uploadRequest: UploadRequest) extends FileState
case object Uploaded extends FileState
case object Successful extends FileState
case object Failed extends FileState
case object VirusDetected extends FileState
case object UnacceptableMimeType extends FileState

object Waiting {

  implicit val format = Json.format[Waiting]
}

object FileState {

  implicit val format = Variants.format[FileState]
}