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
import connectors.Cache
import controllers.actions._
import forms.MRNFormProvider
import javax.inject.Inject
import pages.MrnEntryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class MrnEntryController @Inject()(val messagesApi: MessagesApi,
                                   authenticate: AuthAction,
                                   requireEori: EORIRequiredAction,
                                   requireContactDetails: ContactDetailsRequiredAction,
                                   getData: DataRetrievalAction,
                                   formProvider: MRNFormProvider,
                                   dataCacheConnector: Cache,
                                   implicit val appConfig: AppConfig)(implicit ec: ExecutionContext) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireContactDetails) {
    implicit req =>
      val populatedForm = req.userAnswers.get(MrnEntryPage).map(form.fill).getOrElse(form)

      Ok(views.html.mrn_entry(populatedForm))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireContactDetails).async {
    implicit req =>

      form.bindFromRequest().fold(
        errorForm =>
          Future.successful(BadRequest(views.html.mrn_entry(errorForm))),

        value => {
          val cacheMap = req.userAnswers.set(MrnEntryPage, value).cacheMap

          dataCacheConnector.save(cacheMap).map { _ =>
            Redirect(routes.FileWarningController.onPageLoad())
          }
        }
      )
    }
}
