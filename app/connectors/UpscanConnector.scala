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

import java.io.{File, PrintWriter}
import java.util.UUID

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ContactDetails, UploadRequest}
import play.api.Logger
import play.api.libs.ws.{BodyWritable, DefaultWSProxyServer, InMemoryBody, WSClient}
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._
import play.core.formatters.Multipart
import akka.stream.Materializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class UpscanConnector @Inject()(conf:AppConfig, wsClient: WSClient)(implicit ec: ExecutionContext, materializer: Materializer) {

  def upload(upload: UploadRequest, contactDetails: ContactDetails)= {
    val settings = conf.proxy

    Logger.warn(s"Connecting to proxy = ${settings.proxyRequiredForThisEnvironment}")
    Logger.warn(s" Proxy settings: ${settings.host} ${settings.port} ${settings.protocol} ${settings.username} ${settings.password.take(5)}")
    val proxy = DefaultWSProxyServer(settings.host, settings.port, Some(settings.protocol), Some(settings.username), Some(settings.password))

    val preparedRequest = wsClient.url(upload.href).withFollowRedirects(false)

    val req = if (settings.proxyRequiredForThisEnvironment) preparedRequest.withProxyServer(proxy) else preparedRequest

    val dataparts = upload.fields.map {
      case (name, value) => DataPart(name, value)
    }

    implicit val multipartBodyWriter: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, _]], _]] = {
      val boundary = Multipart.randomBoundary()
      val contentType = s"multipart/form-data; boundary=$boundary"
      BodyWritable(
        body => {
          val byteString = Multipart.transform(body, boundary).runFold(ByteString.empty)(_ ++ _)
          InMemoryBody(Await.result(byteString, Duration.Inf))// <- //use something sensible as a timeout
        },
        contentType
      )
    }


    val filePart = FilePart("file", fileName, Some("text/plain"), FileIO.fromPath(toFile(contactDetails).toPath))

    Logger.warn(s"Upload url: ${req.url}")
    Logger.warn(s"Upload URI: ${req.uri}")
    Logger.warn(s"Upload Headers: ${req.headers}")
    Logger.warn(s"Upload Body: ${req.body}")

    req.post[Source[MultipartFormData.Part[Source[ByteString, _]], _]](Source(dataparts ++ List(filePart)))(multipartBodyWriter)
  }

  private def fileName = s"contact_details_${UUID.randomUUID().toString}.txt"

  def toFile(contactDetails: ContactDetails) = {
    val uploadFile = File.createTempFile("cds-file-upload-ui", "")

    new PrintWriter(uploadFile) {
      try {
        write(contactDetails.toString())
      } finally {
        close()
      }
    }

    uploadFile
  }
}
