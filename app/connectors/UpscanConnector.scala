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
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ContactDetails, UploadRequest}
import play.api.Logger
import play.api.libs.ws.{DefaultWSProxyServer, WSClient}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._

import scala.concurrent.ExecutionContext

@Singleton
class UpscanConnector @Inject()(conf:AppConfig, wsClient: WSClient)(implicit ec: ExecutionContext) {

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

    val filePart = FilePart("file", fileName, Some("text/plain"), FileIO.fromPath(toFile(contactDetails).toPath))

    Logger.warn(s"Upload url: ${req.url}")
    Logger.warn(s"Upload URI: ${req.uri}")
    Logger.warn(s"Upload Headers: ${req.headers}")
    Logger.warn(s"Upload Body: ${req.body}")

    req.post(Source(dataparts ++ List(filePart)))
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
