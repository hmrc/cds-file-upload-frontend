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
import com.google.inject.Singleton

import scala.collection.JavaConverters._
import javax.inject.Inject
import models.{ContactDetails, UploadRequest}
import play.api.libs.ws._
import play.api.mvc.MultipartFormData.{DataPart, FilePart, Part}

import scala.concurrent.Future

@Singleton
class S3Connector @Inject()(ws: WSClient) {

  def createFileName: String = s"contact_details_${UUID.randomUUID().toString()}.txt"

  def myBuilder(contactDetails: ContactDetails, uploadRequest: UploadRequest) = {
    val fileName = createFileName
    val r = uploadRequest.fields.toList.map(field => new DataPart(field._1, field._2))
    new FilePart("file", fileName, Some("text/plain"), FileIO.fromPath(toFile(contactDetails.toString(), fileName).toPath)) :: r
  }

  def uploadContactDetailsToS3(contactDetails: ContactDetails, uploadRequest: UploadRequest) = {

    val response = ws.url(uploadRequest.href).post(Source(myBuilder(contactDetails, uploadRequest)))
    response
  }

  def toFile(contents: String, fileName: String): File = {

    val uploadFile = File.createTempFile(fileName, "")

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