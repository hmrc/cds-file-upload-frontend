/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.ControllerSpecBase
import models.{ContactDetails, FileUploadAnswers, MRN}
import models.requests.{AuthenticatedRequest, ContactDetailsRequest, MrnRequest}
import play.api.http.Status
import play.api.mvc.Result
import testdata.CommonTestData.*

import scala.concurrent.Future

class ContactDetailsRequiredActionSpec extends ControllerSpecBase {

  private val action: ActionTestWrapper = new ActionTestWrapper

  "ContactDetailsRequiredAction" when {

    "contact details have not been provided" must {
      "redirect user to error" in {
        val noneContactDetails: Option[ContactDetails] = None

        val mrnRequest = MrnRequest(
          AuthenticatedRequest(fakeSessionDataRequest, signedInUser),
          FileUploadAnswers(eori = "eori", uuid = "uuid", contactDetails = noneContactDetails),
          new MRN(mrn)
        )

        action.callRefine(mrnRequest).futureValue match {
          case Right(contactDetailsRequest) =>
            fail(s"Should not have got '$contactDetailsRequest'")
          case Left(error) =>
            assert(error.header.status == Status.SEE_OTHER)
            assert(error.header.headers("Location") == "/error")
        }
      }
    }

    "contact details have been provided" must {
      "build ContactDetailsRequest with the provided contact details" in {
        val contactDetails = ContactDetails("name", "company", "01010101010")

        val mrnRequest = MrnRequest(
          AuthenticatedRequest(fakeSessionDataRequest, signedInUser),
          FileUploadAnswers(eori = "eori", uuid = "uuid", contactDetails = Some(contactDetails)),
          new MRN(mrn)
        )

        action.callRefine(mrnRequest).futureValue match {
          case Right(contactDetailsRequest) =>
            assert(contactDetailsRequest.contactDetails == contactDetails)
          case Left(_) =>
            fail("Should not have errored")
        }
      }
    }
  }

  class ActionTestWrapper extends ContactDetailsRequiredActionImpl(mcc) {
    def callRefine[A](request: MrnRequest[A]): Future[Either[Result, ContactDetailsRequest[A]]] = refine(request)
  }

}
