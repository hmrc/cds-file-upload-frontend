/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import java.io.StringReader

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}

import scala.util.control.NonFatal
import scala.xml._

trait XmlBehaviours {

  val fileUploadRequest: Seq[String] = Seq("/FileUploadRequest.xsd")
  val fileUploadResponse: Seq[String] = Seq("/FileUploadResponse.xsd")

  def validateFileUploadRequest(elem: => Elem): Boolean =
    validateXml(elem, fileUploadRequest)

  def validateFileUploadResponse(elem: => Elem): Boolean =
    validateXml(elem, fileUploadResponse)

  def validateXml(elem: => Elem, schemas: Seq[String]): Boolean =
    try {
      validateAgainstSchemaResources(elem, schemas)
      true
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        false
    }

  private def validateAgainstSchemaResources(xml: Elem, schemas: Seq[String]): Unit = {
    val schema: Schema = {
      val sources = schemas.map(res => getClass.getResource(res).toString).map(systemId => new StreamSource(systemId)).toArray[Source]
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(sources)
    }

    val validator = schema.newValidator()
    validator.validate(new StreamSource(new StringReader(xml.mkString)))
  }
}
