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
import play.api.libs.Files.TemporaryFile

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.Try

@Singleton
class UpscanS3Connector() {

  def upload(template: UploadRequest, file: TemporaryFile): Future[Unit] = {
    val builder: MultipartEntityBuilder = MultipartEntityBuilder.create

    template.fields.foreach(entry => builder.addPart(entry._1, new StringBody(entry._2, ContentType.TEXT_PLAIN)))
    builder.addPart("file", new FileBody(file.file))

    val request: HttpPost = new HttpPost(template.href)
    request.setEntity(builder.build())

    val client = HttpClientBuilder.create.build

    val attempt = Try(client.execute(request)).map { response: HttpResponse =>
      val code = response.getStatusLine.getStatusCode
      if (code >= 200 && code < 300) {
        successful(() : Unit)
      } else {
        failed(
          new RuntimeException(
            s"Bad AWS response with status [$code] body [${EntityUtils.toString(response.getEntity)}]"
          )
        )
      }
    }

    client.close()
    attempt.get
  }

}
