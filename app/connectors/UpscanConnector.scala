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

import java.util.UUID

import javax.inject.Singleton
import models.{ContactDetails, UploadRequest}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClientBuilder
import play.api.Logger

import scala.util.{Failure, Success, Try}

@Singleton
class UpscanConnector() {

  def upload(upload: UploadRequest, contactDetails: ContactDetails): Try[Int] = {
    val builder = MultipartEntityBuilder.create

    upload.fields.foreach {
      case (name, value) => builder.addPart(name, new StringBody(value, ContentType.TEXT_PLAIN))
    }

    builder.addBinaryBody("file", contactDetails.toString.getBytes("UTF-8") , ContentType.DEFAULT_BINARY, fileName)

    val request = new HttpPost(upload.href)
    request.setEntity(builder.build())

    val client = HttpClientBuilder.create.disableRedirectHandling().build

    val attempt = Try(client.execute(request)) match {
      case Success(response) =>
        val code = response.getStatusLine.getStatusCode
        val isSuccessRedirect = response.getHeaders("Location").headOption.exists(_.getValue.contains("upscan-success"))
        Logger.info(s"Upscan upload contact details responded with: ${code}")
        if (isSuccessRedirect)
          Success(code)
        else
          Failure(new Exception(s"Uploading contact details to s3 failed"))
      case Failure(ex) =>
        Logger.error(ex.getMessage, ex)
        Failure(ex)
    }

    client.close()

    attempt
  }

  private def fileName = s"contact_details_${UUID.randomUUID().toString}.txt"

}
