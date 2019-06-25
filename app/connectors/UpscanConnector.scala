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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ContactDetails, UploadRequest}
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, NTCredentials}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.impl.client.{BasicCredentialsProvider, HttpClientBuilder, ProxyAuthenticationStrategy}
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class UpscanConnector @Inject()(conf:AppConfig, wsClient: WSClient)(implicit ec: ExecutionContext) {


  val proxySettings = conf.proxy

  val ntCreds = new NTCredentials(proxySettings.username, proxySettings.password, "cds-file-upload-frontend", "mdtp")
  val credsProvider = new BasicCredentialsProvider()
  credsProvider.setCredentials(new AuthScope(proxySettings.host, proxySettings.port), ntCreds)

  def upload(upload: UploadRequest, contactDetails: ContactDetails): Try[Int] = {

    val builder = MultipartEntityBuilder.create

    upload.fields.foreach {
      case (name, value) => builder.addPart(name, new StringBody(value, ContentType.TEXT_PLAIN))
    }


    builder.addPart("file", new FileBody(toFile(contactDetails), ContentType.DEFAULT_BINARY, fileName))

    val request = new HttpPost(upload.href)
    request.setEntity(builder.build())

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

  private def client = proxySettings.proxyRequiredForThisEnvironment match {
    case true =>
      HttpClientBuilder.create
        .disableRedirectHandling()
        .setProxy(new HttpHost(proxySettings.host, proxySettings.port))
        .setDefaultCredentialsProvider(credsProvider)
        .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy())
        .build()
    case _ =>
      HttpClientBuilder.create
        .disableRedirectHandling()
        .build()
  }

}
