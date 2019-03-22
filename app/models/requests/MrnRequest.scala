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

package models.requests

import models.{ContactDetails, FileUploadResponse, MRN, UserAnswers}
import play.api.mvc.WrappedRequest

case class OptionalDataRequest[A](request: EORIRequest[A],
                                  userAnswers: Option[UserAnswers]) extends WrappedRequest[A](request)


case class MrnRequest[A](request: EORIRequest[A], userAnswers: UserAnswers, mrn: MRN) extends WrappedRequest[A](request)

case class FileUploadResponseRequest[A](request: EORIRequest[A], userAnswers: UserAnswers, fileUploadResponse: FileUploadResponse) extends WrappedRequest[A](request)

case class ContactDetailsRequest[A](request: EORIRequest[A], userAnswers: UserAnswers, contactDetails: ContactDetails) extends WrappedRequest[A](request)

