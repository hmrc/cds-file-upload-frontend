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

package controllers

import java.io.StringWriter

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.{BeanDeserializerModifier, ContextualDeserializer}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.xml.annotation.{JacksonXmlElementWrapper, JacksonXmlRootElement}
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.xml.{Elem, XML}

// TODO move Jackson-based model classes for customs declarations API messages into wco-dec domain library

@JsonIgnoreProperties(Array("_xml"))
@JacksonXmlRootElement(localName = "FileUploadRequest")
case class BatchFileUploadRequest(@JsonProperty("DeclarationID")
                                  declarationId: String,

                                  @JsonProperty("FileGroupSize")
                                  fileGroupSize: Int,

                                  @JsonProperty("File")
                                  @JacksonXmlElementWrapper(localName = "Files")
                                  files: Seq[BatchFileUploadFile]) extends JacksonMapper

object BatchFileUploadRequest extends JacksonMapper {

  // cannot use Jackson for deserialization due to issue with element wrapper and Scala case classes
  // haven't bothered writing a custom deserializer to resolve the issue because IRL we will not need deserialization here
  def fromXml(xml: String): BatchFileUploadRequest = {
    val x: Elem = XML.loadString(xml)
    val declarationId = (x \ "DeclarationID").text.trim
    val fileGroupSize = (x \ "FileGroupSize").text.trim.toInt
    val files: Seq[BatchFileUploadFile] = (x \ "Files" \ "_").theSeq.collect {
      case file =>
        val fileSequenceNumber = (file \ "FileSequenceNo").text.trim.toInt
        val documentType = (file \ "DocumentType").text.trim
        BatchFileUploadFile(fileSequenceNumber, documentType)
    }
    BatchFileUploadRequest(declarationId, fileGroupSize, files)
  }

}

case class BatchFileUploadFile(@JsonProperty("FileSequenceNo")
                               fileSequenceNo: Int,

                               @JsonProperty("DocumentType")
                               documentType: String)

@JsonIgnoreProperties(Array("_xml"))
@JacksonXmlRootElement(localName = "FileUploadResponse")
case class BatchFileUploadResponse(@JsonProperty("File")
                                   @JacksonXmlElementWrapper(localName = "Files")
                                   files: List[BatchFile]) extends JacksonMapper

object BatchFileUploadResponse {

  def apply(xml: String): BatchFileUploadResponse = {
    val x: Elem = XML.loadString(xml)
    val files: List[BatchFile] = (x \ "Files" \ "_").theSeq.collect {
      case file =>
        val reference = (file \ "reference").text.trim
        val href = (file \ "uploadRequest" \ "href").text.trim
        val fields: Map[String, String] = (file \ "uploadRequest" \ "fields" \ "_").theSeq.collect {
          case field =>
            field.label -> field.text.trim
        }.toMap
        BatchFile(reference, UploadRequest(href, fields))
    }.toList
    BatchFileUploadResponse(files)
  }

}

case class BatchFile(reference: String, uploadRequest: UploadRequest)

case class UploadRequest(href: String,

                         @JacksonXmlElementWrapper(localName = "fields")
                         fields: Map[String, String])

trait JacksonMapper {

  private val _modxml = new JacksonXmlModule()

  protected val _xml: XmlMapper = new XmlMapper(_modxml)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    .registerModule(DefaultScalaModule)
    .registerModule(CustomSeqModule)
    .asInstanceOf[XmlMapper]

  def toXml: String = {
    val sw = new StringWriter()
    _xml.writeValue(sw, this)
    sw.toString
  }

}

object CustomSeqModule extends SimpleModule {
  setDeserializerModifier(SeqDeserializationModifier)
}

object SeqDeserializationModifier extends BeanDeserializerModifier {

  override def modifyCollectionLikeDeserializer(config: DeserializationConfig,
                                                `type`: CollectionLikeType,
                                                beanDesc: BeanDescription,
                                                deserializer: JsonDeserializer[_]): JsonDeserializer[_] = new JsonDeserializer[Seq[_]] with ContextualDeserializer {

    override def deserialize(p: JsonParser, ctx: DeserializationContext): Seq[_] =
      deserializer.deserialize(p, ctx).asInstanceOf[Seq[_]]

    override def createContextual(ctx: DeserializationContext, prop: BeanProperty): JsonDeserializer[_] =
      modifyCollectionLikeDeserializer(config, `type`, beanDesc, deserializer.asInstanceOf[ContextualDeserializer].createContextual(ctx, prop))

    override def getNullValue(ctx: DeserializationContext): Seq[_] = Seq.empty
  }

}
