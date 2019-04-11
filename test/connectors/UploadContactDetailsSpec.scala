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

package connectors


import base.SpecBase
import models.{ContactDetails, UploadRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class UploadContactDetailsSpec extends SpecBase with MockitoSugar {

  val mockS3Connector = mock[UpscanS3Connector]

  val UploadContactDetails = new UploadContactDetails(mockS3Connector)

  "UploadContactDetails" must {

    "UploadContactDetailsto s3" in {

      val contactDetails = ContactDetails("name", "companyName", "phoneNumber", "email")
      val uploadRequest = UploadRequest("someUrl", Map("k" -> "v"))

      UploadContactDetails.apply(contactDetails, uploadRequest)

      when(mockS3Connector.upload(any(), any())).thenReturn(Future.successful())

      verify(mockS3Connector).upload(any(), any())
    }
  }
}
