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

import com.google.inject.Singleton

import scala.collection.JavaConverters._
import javax.inject.Inject
import models.{ContactDetails, UploadRequest}
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.request.body.multipart.{FilePart, Part, StringPart}
import play.api.libs.ws._

@Singleton
class S3Connector @Inject()(ws: WSClient) {

  val asyncHttpClient: AsyncHttpClient = ws.underlying

  def myBuilder(contactDetails: ContactDetails, uploadRequest: UploadRequest): List[Part] = {
    val r = uploadRequest.fields.toList.map(field => new StringPart(field._1, field._2, "UTF-8"))
    new FilePart("file", toFile(contactDetails.toString(), s"contact_details_${UUID.randomUUID().toString()}")) :: r
  }

  def uploadContactDetailsToS3(contactDetails: ContactDetails, uploadRequest: UploadRequest) = {
    val postBuilder = asyncHttpClient.preparePost(uploadRequest.href)
    val x = myBuilder(contactDetails, uploadRequest).asJava
    val response = asyncHttpClient.executeRequest(postBuilder.setBodyParts(x).build())
    response
  }

  def toFile(contents: String, fileName: String): File = {

    val uploadFile = File.createTempFile(fileName, ".txt")

    uploadFile.deleteOnExit() // Don't do this, need to delete after file has been used
    new PrintWriter(uploadFile) {
      try {
        write(contents)
      } finally {
        close()
      }
    }
    uploadFile
  }
}