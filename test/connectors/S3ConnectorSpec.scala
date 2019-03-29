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

import java.io.{BufferedReader, FileReader}
import java.util.UUID

import base.SpecBase
import controllers.actions.FakeActions
import generators.Generators
import models.{ContactDetails, UploadRequest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import play.api.libs.ws._

class S3ConnectorSpec extends SpecBase
  with MockitoSugar
  with PropertyChecks
  with Generators
  with BeforeAndAfterEach
  with ScalaFutures
  with FakeActions {

  val ws = app.injector.instanceOf[WSClient]

  val connector = new S3Connector(ws)

  "S3Connector" must {

    "convert contact details to a file" in {

      forAll { (contactDetails: ContactDetails, uploadRequest: UploadRequest) =>

        val data = contactDetails.toString()
        val contactDetailsFile = connector.toFile(data, s"contact_details_${UUID.randomUUID().toString()}")

        val br = new BufferedReader(new FileReader(contactDetailsFile))
        val st = br.readLine()

        if (!st.isEmpty) {
          st mustBe s"Name: ${contactDetails.name}"
        }
      }
    }
  }
}
