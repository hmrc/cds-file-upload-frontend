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

import akka.stream.scaladsl.Source
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ContactDetails, UploadRequest}
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}

import scala.concurrent.Future

@Singleton
class UpscanConnector @Inject()(conf:AppConfig, wsClient: WSClient) {

  def upload(upload: UploadRequest, contactDetails: ContactDetails): Future[WSResponse] = {
    val settings = conf.prodProxy.proxy
    val proxy = DefaultWSProxyServer(settings.host, settings.port, Some(settings.protocol), Some(settings.username), Some(settings.password))

    val preparedRequest = wsClient.url(upload.href).withFollowRedirects(false)

    val req = if (settings.proxyRequiredForThisEnvironment) preparedRequest.withProxyServer(proxy) else preparedRequest

    val dataparts = upload.fields.map {
      case (name, value) => DataPart(name, value)
    }
    val filePart = FilePart("file", fileName, Some("text/plain") , contactDetails.toString.getBytes("UTF-8"))
    import play.api.libs.ws.DefaultBodyReadables._
    import play.api.libs.ws.DefaultBodyWritables._
    req.post(Source(filePart :: dataparts :: Nil))

  }

  private def fileName = s"contact_details_${UUID.randomUUID().toString}.txt"

}
