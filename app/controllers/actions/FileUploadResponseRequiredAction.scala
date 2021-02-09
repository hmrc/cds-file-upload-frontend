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

package controllers.actions

import javax.inject.Inject
import controllers.routes
import models.requests.{DataRequest, FileUploadResponseRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}

import scala.concurrent.{ExecutionContext, Future}

class FileUploadResponseRequiredAction @Inject()(implicit mcc: MessagesControllerComponents)
    extends ActionRefiner[DataRequest, FileUploadResponseRequest] {

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  private lazy val onError = Redirect(routes.ErrorPageController.error())

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, FileUploadResponseRequest[A]]] = {

    val req = for {
      response <- request.userAnswers.fileUploadResponse
    } yield FileUploadResponseRequest(request.request, request.userAnswers, response)

    Future.successful(req.toRight(onError))
  }
}
