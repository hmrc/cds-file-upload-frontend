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

package controllers

import java.util.UUID

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.collection.mutable
import scala.concurrent.Future

@Singleton
class FileUploadController @Inject()(val messagesApi: MessagesApi, httpClient: HttpClient, implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val mongo = mutable.Map[String, BatchFileUploadResponse]()
	
	val displayFileInfoForm = Action { implicit req =>
		Ok(views.html.file_info(Forms.fileInfoForm))
	}

	val handleFileInfoForm = Action.async { implicit req =>
    Forms.fileInfoForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.file_info(errors))),
      success => {
        val xml = BatchFileUploadRequest(success.mrn, success.count, (1 to success.count).map(i => BatchFileUploadFile(i, "DOCUMENT_TYPE"))).toXml
        httpClient.POSTString[HttpResponse](appConfig.microservice.services.customsDeclarations.batchUploadEndpoint, xml, Seq(HeaderNames.CONTENT_TYPE -> ContentTypes.XML)).map { response =>
          // TODO store batch in keystore
          Logger.info("RESPONSE: " + response.body)
          val batch = BatchFileUploadResponse(response.body)
          val batchId = UUID.randomUUID().toString
          mongo.put(batchId, batch)
          Redirect(routes.FileUploadController.displayFileUploadForm(batchId, 1))
        }
      }
    )
  }
  
  // TODO
  val displayFileSizeForm = Action { implicit req =>
    Ok(views.html.file_size())
  }
  
  // TODO
  val handleFileSizeForm = Action { implicit req =>
    Ok(views.html.file_size())
  }

  def displayFileUploadForm(batchId: String, batchFileNumber: Int) = Action { implicit req =>
    mongo.get(batchId).map { batch =>
      if (batch.files.size < batchFileNumber) NotFound
      else {
        val file: BatchFile = batch.files(batchFileNumber - 1)
        val next = if (batchFileNumber == batch.files.size) routes.FileUploadController.displayConfirmationPage(batchId) else routes.FileUploadController.displayFileUploadForm(batchId, batchFileNumber + 1)
        Ok(views.html.file_upload(file, next, batchFileNumber, batch.files.size))
      }
    }.getOrElse(NotFound)
  }

  def displayConfirmationPage(batchId: String) = Action { implicit req =>
    mongo.get(batchId).map { batch =>
      Ok(views.html.file_receipts(batch))
    }.getOrElse(NotFound)
  }
	
}

case class FileInfo(mrn: String, count: Int)
