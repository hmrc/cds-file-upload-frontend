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
import connectors.DataCacheConnector
import controllers.actions._
import forms.mappings.ContactDetailsMapping._
import javax.inject.Inject
import models._
import pages.ContactDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.contact_details

import scala.concurrent.Future

@Singleton
class ContactDetailsController @Inject()(
                                          val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          requireEori: EORIAction,
                                          getData: DataRetrievalAction,
                                          dataCacheConnector: DataCacheConnector,
                                          implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val form = Form(contactDetailsMapping)

  def onPageLoad: Action[AnyContent] = (authenticate andThen requireEori andThen getData) { implicit req =>

    val populatedForm =
      req.userAnswers
        .flatMap(
          _.get(ContactDetailsPage)
            .map(form.fill))
        .getOrElse(form)
    Ok(contact_details(populatedForm))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen requireEori andThen getData).async {
    implicit req =>

      val userAnswers = req.userAnswers.getOrElse(UserAnswers(req.request.user.internalId))

      form.bindFromRequest().fold(
        errorForm =>
          Future.successful(BadRequest(contact_details(errorForm))),

        value => {
          val cacheMap = userAnswers.set(ContactDetailsPage, value).cacheMap

          dataCacheConnector.save(cacheMap).map { _ =>
            Redirect(routes.MrnEntryController.onPageLoad())
          }
        }
      )
  }
}
