/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.AppConfig
import javax.inject.{Inject, Singleton}
import metrics.SfusMetrics
import metrics.MetricIdentifiers.fileUploadMetric
import models.{ContactDetails, UploadRequest}
import play.api.Logger
import play.api.libs.ws.{BodyWritable, DefaultWSProxyServer, InMemoryBody, WSClient}
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.core.formatters.Multipart

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class UpscanConnector @Inject()(conf: AppConfig, wsClient: WSClient, metrics: SfusMetrics)(
  implicit ec: ExecutionContext,
  materializer: Materializer
) {

  private val logger = Logger(this.getClass)

  def upload(upload: UploadRequest, contactDetails: ContactDetails) = {
    val settings = conf.proxy

    logger.info(s"Connecting to proxy = ${settings.proxyRequiredForThisEnvironment}")
    logger.info(s"Proxy settings: ${settings.host} ${settings.port} ${settings.protocol} ${settings.username} ${settings.password.take(5)}")
    val proxy = DefaultWSProxyServer(settings.host, settings.port, Some(settings.protocol), Some(settings.username), Some(settings.password))

    val preparedRequest = wsClient.url(upload.href).withFollowRedirects(false)

    val req = if (settings.proxyRequiredForThisEnvironment) preparedRequest.withProxyServer(proxy) else preparedRequest

    val dataParts = upload.fields.map { case (name, value) => DataPart(name, value) }

    implicit val multipartBodyWriter: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, _]], _]] = {
      val boundary = Multipart.randomBoundary()
      val contentType = s"multipart/form-data; boundary=$boundary"
      BodyWritable(body => {
        val byteString = Multipart.transform(body, boundary).runFold(ByteString.empty)(_ ++ _)
        InMemoryBody(Await.result(byteString, Duration(90, SECONDS)))
      }, contentType)
    }

    val filePart = FilePart("file", fileName, Some("text/plain"), Source.single(ByteString(contactDetails.toString)))

    logger.info(s"Upload url: ${req.url}")
    logger.info(s"Upload URI: ${req.uri}")
    logger.info(s"Upload Headers: ${req.headers}")

    val body = dataParts ++ List(filePart)
    val timer = metrics.startTimer(fileUploadMetric)

    req.post[Source[MultipartFormData.Part[Source[ByteString, _]], _]](Source(body))(multipartBodyWriter).map { response =>
      timer.stop()
      metrics.incrementCounter(fileUploadMetric)
      response
    }
  }

  private def fileName = s"contact_details_${UUID.randomUUID().toString}.txt"
}
