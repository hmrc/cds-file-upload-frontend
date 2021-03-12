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

package views

import base.OverridableInjector
import config.SecureMessagingConfig
import controllers.actions.FakeActions
import controllers.routes
import forms.MRNFormProvider
import models.MRN
import models.requests.{AuthenticatedRequest, SignedInUser}
import org.jsoup.nodes.Document
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.api.inject.bind
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.behaviours.StringViewBehaviours
import views.html.mrn_entry

class MrnEntrySpec extends DomAssertions with FakeActions with StringViewBehaviours[MRN] with ScalaCheckPropertyChecks {

  val form = new MRNFormProvider()()

  val page = instanceOf[mrn_entry]

  val view = page(form)(fakeRequest.withCSRFToken, messages)

  val messagePrefix = "mrnEntryPage"

  def createViewUsingForm: Form[MRN] => HtmlFormat.Appendable = form => page(form)(fakeRequest.withCSRFToken, messages)

  "MRN Entry Page" must {
    behave like normalPage(() => view, messagePrefix)

    behave like stringPage(createViewUsingForm, "value", messagePrefix)

    "include the hint text above the text box" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.getElementsByClass("govuk-hint").first.text.contains(messages("mrnEntryPage.hint"))
    }

    "include the 'Sign out' link if the user is authorised" in {
      forAll { user: SignedInUser =>
        val view = page(form)(AuthenticatedRequest(fakeRequest.withCSRFToken, user), messages)
        assertSignoutLinkIsIncluded(view)
      }
    }

    "display the 'Back' link" when {

      "the feature flag for SecureMessaging is disabled" in {
        assertBackLinkIsIncluded(document(false), routes.StartController.displayStartPage.url)
      }

      "the feature flag for SecureMessaging is enabled" in {
        assertBackLinkIsIncluded(document(true), routes.ChoiceController.onPageLoad.url)
      }

      def document(enabled: Boolean): Document = {
        val secureMessagingConfig = mock[SecureMessagingConfig]
        val injector = new OverridableInjector(bind[SecureMessagingConfig].toInstance(secureMessagingConfig))
        val secureMessagingFlagMock = new SecureMessagingFeatureActionMock(secureMessagingConfig)
        if (enabled) secureMessagingFlagMock.enableSecureMessagingFeature else secureMessagingFlagMock.disableSecureMessagingFeature
        val page = injector.instanceOf[mrn_entry]
        asDocument(page(form)(fakeRequest.withCSRFToken, messages))
      }
    }
  }
}
