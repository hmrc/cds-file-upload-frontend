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

import javax.inject.Singleton
import models.UploadRequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.http.Status._

import scala.util.{Failure, Success, Try}

@Singleton
class UpscanConnector() {

  def upload(upload: UploadRequest, file: TemporaryFile, fileName: String): Try[Int] = {
    val builder = MultipartEntityBuilder.create

    upload.fields.foreach {
      case (name, value) => builder.addPart(name, new StringBody(value, ContentType.TEXT_PLAIN))
    }

    builder.addPart("file", new FileBody(file.file, ContentType.DEFAULT_BINARY, fileName))

    val request = new HttpPost(upload.href)
    request.setEntity(builder.build())

    val client = HttpClientBuilder.create.disableRedirectHandling().build

    val attempt = Try(client.execute(request)) match {
      case Success(response) =>
        val code = response.getStatusLine.getStatusCode
        Logger.info(s"Upscan upload contact details responsed with: ${code}")
        if (code == SEE_OTHER) Success(code) else Failure(new Exception(s"Response code was not 303 but: $code"))
      case Failure(ex) =>
        Logger.error(ex.getMessage, ex)
        Failure(ex)
    }

    client.close()

    attempt
  }
}
