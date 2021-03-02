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

package controllers

import com.google.inject.Singleton
import controllers.actions._
import forms.mappings.ContactDetailsMapping._
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.contact_details

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject()(
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireMrn: MrnRequiredAction,
  verifiedEmail: VerifiedEmailAction,
  answersConnector: AnswersService,
  mcc: MessagesControllerComponents,
  contactDetails: contact_details
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val form = Form(contactDetailsMapping)

  def onPageLoad: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData andThen requireMrn) { implicit req =>
    val populatedForm = req.userAnswers.contactDetails.fold(form)(form.fill)
    Ok(contactDetails(populatedForm, req.mrn))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen verifiedEmail andThen getData andThen requireMrn).async { implicit req =>
    form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(contactDetails(errorForm, req.mrn))),
        contactDetails => {
          answersConnector.upsert(req.userAnswers.copy(contactDetails = Some(contactDetails))).map { _ =>
            Redirect(routes.HowManyFilesUploadController.onPageLoad())
          }
        }
      )
  }
}
