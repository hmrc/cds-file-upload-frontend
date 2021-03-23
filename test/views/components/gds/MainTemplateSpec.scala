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

package views.components.gds

import base.{OverridableInjector, SpecBase}
import config.{AppConfig, SecureMessagingConfig}
import org.jsoup.nodes.Document
import org.mockito.Mockito.{reset, when}
import play.api.inject.bind
import play.twirl.api.HtmlFormat
import views.Title
import views.html.components.gds.gdsMainTemplate
import views.matchers.ViewMatchers
import scala.collection.JavaConverters._

class MainTemplateSpec extends SpecBase with ViewMatchers {

  private val secureMessagingConfig = mock[SecureMessagingConfig]
  private val injector = new OverridableInjector(bind[SecureMessagingConfig].toInstance(secureMessagingConfig))

  override implicit lazy val appConfig: AppConfig = injector.instanceOf[AppConfig]
  private val mainTemplate = injector.instanceOf[gdsMainTemplate]
  private val testContent = HtmlFormat.empty

  private def createView(withNavigationBanner: Boolean = false, withFileUploadFlag: Boolean = false): Document =
    mainTemplate(title = Title("common.service.name"), withNavigationBanner = withNavigationBanner, withFileUploadValidation = withFileUploadFlag)(
      testContent
    )(fakeRequest, messages)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(secureMessagingConfig)
  }

  override def afterEach(): Unit = {
    reset(secureMessagingConfig)
    super.afterEach()
  }

  "Main Template" when {

    "SecureMessagingConfig on isSecureMessagingEnabled returns true" should {

      "display NavigationBanner" when {
        "withNavigationBanner flag set to true" in {
          when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

          val view: Document = createView(withNavigationBanner = true)

          view must containElementWithID("navigation-banner")
        }
      }

      "not display NavigationBanner" when {
        "withNavigationBanner flag set to false" in {
          when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(true)

          val view: Document = createView(withNavigationBanner = false)

          view mustNot containElementWithID("navigation-banner")
        }
      }
    }

    "SecureMessagingConfig on isSecureMessagingEnabled returns false" should {

      "not display NavigationBanner" when {

        "withNavigationBanner flag set to true" in {
          when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(false)

          val view: Document = createView(withNavigationBanner = true)

          view mustNot containElementWithID("navigation-banner")
        }

        "withNavigationBanner flag set to false" in {
          when(secureMessagingConfig.isSecureMessagingEnabled).thenReturn(false)

          val view: Document = createView(withNavigationBanner = false)

          view mustNot containElementWithID("navigation-banner")
        }
      }
    }

    "fileUploadFlag is set on" should {
      "contain the JQuery validation script files" in {
        val view: Document = createView(withFileUploadFlag = true)

        val scripts = view.getElementsByTag("script").asScala.toSeq
        val scriptAttribs = scripts.map(_.attr("src"))
        scriptAttribs must contain("/assets/javascripts/jquery-3.6.0.min.js")
        scriptAttribs must contain("/assets/javascripts/jquery.validate.min.js")
        scriptAttribs must contain("/assets/javascripts/cdsfileuploadfrontend.js")

        scripts.map(_.data()) must contain("""$("form").validate();""")
      }
    }

    "fileUploadFlag is set off" should {
      "not contain the JQuery validation script files" in {
        val view: Document = createView(withFileUploadFlag = false)

        val scripts = view.getElementsByTag("script").asScala.toSeq
        val scriptAttribs = scripts.map(_.attr("src"))
        scriptAttribs mustNot contain("/assets/javascripts/jquery-3.6.0.min.js")
        scriptAttribs mustNot contain("/assets/javascripts/jquery.validate.min.js")
        scriptAttribs mustNot contain("/assets/javascripts/cdsfileuploadfrontend.js")

        scripts.map(_.data()) mustNot contain("""$("form").validate();""")
      }
    }
  }

}
