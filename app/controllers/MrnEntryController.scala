/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.actions.{AuthAction, DataRetrievalAction, EORIAction}
import forms.MRNFormProvider
import javax.inject.Inject
import models.UserAnswers
import pages.MrnEntryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.mrn_entry

import scala.concurrent.Future


@Singleton
class MrnEntryController @Inject()(
  val messagesApi: MessagesApi,
  authenticate: AuthAction,
  requireEori: EORIAction,
  getData: DataRetrievalAction,
  formProvider: MRNFormProvider,
  dataCacheConnector: DataCacheConnector,
  implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen requireEori andThen getData) { implicit req =>
    val populatedForm =
      req.userAnswers
        .flatMap(
          _.get(MrnEntryPage)
           .map(form.fill))
        .getOrElse(form)

    Ok(mrn_entry(populatedForm))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen requireEori andThen getData).async {
    implicit req =>

      val userAnswers = req.userAnswers.getOrElse(UserAnswers(req.request.user.internalId))

      form.bindFromRequest().fold(
        errorForm =>
          Future.successful(BadRequest(mrn_entry(errorForm))),

        value => {
          val cacheMap = userAnswers.set(MrnEntryPage, value).cacheMap

          dataCacheConnector.save(cacheMap).map { _ =>
            Redirect(routes.NumberOfFilesController.onPageLoad())
          }
        }
      )
    }
}
