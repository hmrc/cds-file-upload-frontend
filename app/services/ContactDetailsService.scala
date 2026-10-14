/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import connectors.UpscanConnector
import models.*
import models.requests.MrnRequest
import play.api.Logging
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsService @Inject()(customsDeclarationsService: CustomsDeclarationsService,
                                      upscanConnector: UpscanConnector,
                                      answersService: FileUploadAnswersService)(implicit ec: ExecutionContext) extends Logging {

  def uploadContactDetails(request: MrnRequest[AnyContent], contactDetails: ContactDetails)(using HeaderCarrier): Future[Either[Throwable, Unit]] =
    request.userAnswers.batchId match {
      case None =>
        Future.successful(Left(new IllegalStateException("No batchId in the session")))
      case Some(batchId) =>
        uploadContactDetails(request, batchId, contactDetails)
    }

  private def uploadContactDetails(request: MrnRequest[AnyContent], batchId: String, contactDetails: ContactDetails)(
    using HeaderCarrier
  ): Future[Either[Throwable, Unit]] =
    customsDeclarationsService.initiateSingleFileBatch(request.eori, request.mrn, batchId).flatMap { fileUploadResponse =>
      fileUploadResponse.files match {
        case FileUpload(_, Waiting(uploadRequest), _, _) :: Nil =>
          upscanConnector.upload(uploadRequest, contactDetails).flatMap { response =>
            val isSuccessRedirect = response.header("Location").exists(_.contains("upscan-success"))
            if (response.status == SEE_OTHER && isSuccessRedirect) {
              val answers = request.userAnswers.copy(fileUploadResponse = Some(FileUploadResponse(Nil)))
              answersService.findOneAndReplace(answers).map(_ => Right(()))
            } else {
              logger.warn(s"Failed to upload contact details. Response: $response")
              Future.successful(Left(new IllegalStateException("Failed to upload contact details")))
            }
          }

        case other =>
          logger.warn(s"Unable to initiate upload. Expected file upload with waiting but got: $other")
          Future.successful(Left(new IllegalStateException("Unable to initiate upload")))
      }
    }
}
