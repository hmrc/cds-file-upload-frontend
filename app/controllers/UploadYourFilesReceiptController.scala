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

import com.google.inject.Singleton
import config.AppConfig
import controllers.actions._
import javax.inject.Inject
import pages.{ContactDetailsPage, HowManyFilesUploadPage, MrnEntryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files_receipt
import uk.gov.hmrc.play.audit.AuditExtensions._

@Singleton
class UploadYourFilesReceiptController @Inject()(
                                                  val messagesApi: MessagesApi,
                                                  authenticate: AuthAction,
                                                  requireEori: EORIRequiredActionImpl,
                                                  getData: DataRetrievalAction,
                                                  requireResponse: FileUploadResponseRequiredAction,
                                                  auditConnector: AuditConnector,
                                                  implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      def auditDetails = {
        val contactDetails = req.userAnswers.get(ContactDetailsPage).fold(Map.empty[String, String])(cd => Map("fullName" -> cd.name, "companyName" -> cd.companyName, "emailAddress" -> cd.email, "telephoneNumber" -> cd.phoneNumber))
        val eori = Map("eori" -> req.request.eori)
        val mrn = req.userAnswers.get(MrnEntryPage).fold(Map.empty[String, String])(m => Map("mrn" -> m.value))
        val numberOfFiles = req.userAnswers.get(HowManyFilesUploadPage).fold(Map.empty[String, String])(n => Map("numberOfFiles" -> s"${n.value}"))
        val references = req.fileUploadResponse.files.map(_.reference).foldLeft(Map.empty[String,String]){(refs: Map[String, String], fileRef: String) => refs + (s"file${refs.size}" -> fileRef) }
        contactDetails ++ eori ++ mrn ++ numberOfFiles ++ references
      }

      sendDataEvent(transactionName = "trader-submission", detail = auditDetails, eventType = "UploadSuccess")

      Ok(upload_your_files_receipt(req.fileUploadResponse.files.map(_.reference)))
    }


  private val auditSource: String = appConfig.appName
  private val audit: Audit = Audit(auditSource, auditConnector)

  def sendDataEvent(transactionName: String, path: String = "N/A", tags: Map[String, String] = Map.empty, detail: Map[String, String], eventType: String)
                   (implicit hc: HeaderCarrier): Unit = {
    audit.sendDataEvent(DataEvent(
      auditSource,
      eventType,
      tags = hc.toAuditTags(transactionName, path) ++ tags,
      detail = hc.toAuditDetails(detail.toSeq: _*))
    )
  }
}
