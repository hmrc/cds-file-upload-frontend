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

package controllers

import com.google.inject.Singleton
import connectors.AnswersConnector
import controllers.actions._
import forms.MRNFormProvider
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.mrn_entry

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MrnEntryController @Inject()(
  authenticate: AuthAction,
  requireEori: EORIRequiredAction,
  requireContactDetails: ContactDetailsRequiredAction,
  getData: DataRetrievalAction,
  formProvider: MRNFormProvider,
  answersConnector: AnswersConnector,
  mcc: MessagesControllerComponents,
  mrnEntry: mrn_entry
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireContactDetails) { implicit req =>
    val populatedForm = req.userAnswers.mrn.fold(form)(form.fill)
    Ok(mrnEntry(populatedForm))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireContactDetails).async { implicit req =>
    form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(mrnEntry(errorForm))),
        value => {
          answersConnector.upsert(req.userAnswers.copy(mrn = Some(value))).map { _ =>
            Redirect(routes.HowManyFilesUploadController.onPageLoad())
          }
        }
      )
  }
}
