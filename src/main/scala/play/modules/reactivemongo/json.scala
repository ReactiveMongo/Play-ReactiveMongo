/*
 * Copyright 2012-2013 Stephane Godbillon (@sgodbillon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.modules.reactivemongo.json

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPluginException

import reactivemongo.bson._

import reactivemongo.play.json, json.{
  BSONFormats => PlayFormats,
  JSONException
}

import scala.math.BigDecimal.{
  double2bigDecimal,
  int2bigDecimal,
  long2bigDecimal
}

object `package` extends ImplicitBSONHandlers {
  @deprecated("0.11.9", "Use [[reactivemongo.play.json.readOpt]]")
  val readOpt = json.readOpt
}

object BSONFormats extends BSONFormats

/**
 * JSON Formats for BSONValues.
 */
sealed trait BSONFormats extends LowerImplicitBSONHandlers {
  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.PartialFormat]]")
  trait PartialFormat[T <: BSONValue] extends PlayFormats.PartialFormat[T]

  private object PartialFormat {
    implicit class apply[T <: BSONValue](
        underlying: PlayFormats.PartialFormat[T]) extends PartialFormat[T] {

      val partialReads: PartialFunction[JsValue, JsResult[T]] =
        underlying.partialReads

      val partialWrites: PartialFunction[BSONValue, JsValue] =
        underlying.partialWrites

    }
  }

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONDoubleFormat]]")
  implicit val BSONDoubleFormat: PartialFormat[BSONDouble] =
    PlayFormats.BSONDoubleFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONStringFormat]]")
  implicit val BSONStringFormat: PartialFormat[BSONString] =
    PlayFormats.BSONStringFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONDocumentFormat]]")
  class BSONDocumentFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PlayFormats.BSONDocumentFormat(toBSON, toJSON) with PartialFormat[BSONDocument] {
  }

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONDocumentFormat]]")
  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONArrayFormat]]")
  class BSONArrayFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PlayFormats.BSONArrayFormat(toBSON, toJSON) with PartialFormat[BSONArray] {
  }

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONArrayFormat]]")
  implicit object BSONArrayFormat extends BSONArrayFormat(toBSON, toJSON)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONObjectIDFormat]]")
  implicit val BSONObjectIDFormat: PartialFormat[BSONObjectID] =
    PlayFormats.BSONObjectIDFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONBooleanFormat]]")
  implicit val BSONBooleanFormat: PartialFormat[BSONBoolean] =
    PlayFormats.BSONBooleanFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONDateTimeFormat]]")
  implicit val BSONDateTimeFormat: PartialFormat[BSONDateTime] =
    PlayFormats.BSONDateTimeFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONTimestampFormat]]")
  implicit val BSONTimestampFormat: PartialFormat[BSONTimestamp] =
    PlayFormats.BSONTimestampFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONRegexFormat]]")
  implicit val BSONRegexFormat: PartialFormat[BSONRegex] =
    PlayFormats.BSONRegexFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONNullFormat]]")
  implicit val BSONNullFormat: PartialFormat[BSONNull.type] =
    PlayFormats.BSONNullFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONIntegerFormat]]")
  implicit val BSONIntegerFormat: PartialFormat[BSONInteger] =
    PlayFormats.BSONIntegerFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONLongFormat]]")
  implicit val BSONLongFormat: PartialFormat[BSONLong] =
    PlayFormats.BSONLongFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONBinaryFormat]]")
  implicit val BSONBinaryFormat: PartialFormat[BSONBinary] =
    PlayFormats.BSONBinaryFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONSymbolFormat]]")
  implicit val BSONSymbolFormat: PartialFormat[BSONSymbol] =
    PlayFormats.BSONSymbolFormat

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.numberReads]]")
  val numberReads: PartialFunction[JsValue, JsResult[BSONValue]] =
    PlayFormats.numberReads

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.toBSON]]")
  def toBSON(json: JsValue): JsResult[BSONValue] = try {
    PlayFormats.toBSON(json)
  } catch {
    case je: JSONException =>
      throw new ReactiveMongoPluginException(je.getMessage)

    case ex: Throwable =>
      throw new ReactiveMongoPluginException(ex.getMessage, ex)
  }

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.toJSON]]")
  def toJSON(bson: BSONValue): JsValue = try {
    PlayFormats.toJSON(bson)
  } catch {
    case je: JSONException =>
      throw new ReactiveMongoPluginException(je.getMessage)

    case ex: Throwable =>
      throw new ReactiveMongoPluginException(ex.getMessage, ex)
  }
}

object Writers {
  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.Writers.JsPathMongo]]")
  implicit class JsPathMongo(val jp: JsPath) extends AnyVal {
    def writemongo[A](implicit writer: Writes[A]): OWrites[A] =
      reactivemongo.play.json.Writers.JsPathMongo(jp).writemongo[A](writer)
  }
}

@deprecated("0.11.9",
  "Use [[reactivemongo.play.json.JSONSerializationPack]]")
object JSONSerializationPack extends reactivemongo.api.SerializationPack {
  import reactivemongo.play.json.{ JSONSerializationPack => PlayPack }

  import reactivemongo.bson.buffer.{ ReadableBuffer, WritableBuffer }

  type Value = PlayPack.Value
  type ElementProducer = PlayPack.ElementProducer
  type Document = PlayPack.Document
  type Writer[A] = PlayPack.Writer[A]
  type Reader[A] = PlayPack.Reader[A]

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.IdentityReader]]")
  val IdentityReader: Reader[Document] = PlayPack.IdentityReader

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.IdentityWriter]]")
  val IdentityWriter: Writer[Document] = PlayPack.IdentityWriter

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.serialize]]")
  def serialize[A](a: A, writer: Writer[A]): Document =
    PlayPack.serialize[A](a, writer)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.deserialize]]")
  def deserialize[A](document: Document, reader: Reader[A]): A =
    PlayPack.deserialize[A](document, reader)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.writeToBuffer]]")
  def writeToBuffer(buffer: WritableBuffer, document: Document): WritableBuffer = PlayPack.writeToBuffer(buffer, document)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.readFromBuffer]]")
  def readFromBuffer(buffer: ReadableBuffer): Document =
    PlayPack.readFromBuffer(buffer)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.writer]]")
  def writer[A](f: A => Document): Writer[A] = PlayPack.writer[A](f)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.JSONSerializationPack.isEmpty]]")
  def isEmpty(document: Document): Boolean = PlayPack.isEmpty(document)
}

import reactivemongo.bson.{
  BSONDocument,
  BSONDocumentReader,
  BSONDocumentWriter
}

object ImplicitBSONHandlers extends ImplicitBSONHandlers

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
@deprecated("0.11.9",
  "Use [[reactivemongo.play.json.BSONFormats]]")
sealed trait ImplicitBSONHandlers extends BSONFormats {
  implicit object JsObjectWriter extends BSONDocumentWriter[JsObject] {
    def write(obj: JsObject): BSONDocument =
      BSONFormats.BSONDocumentFormat.partialReads(obj).get
  }

  implicit object JsObjectReader extends BSONDocumentReader[JsObject] {
    def read(document: BSONDocument) =
      BSONFormats.BSONDocumentFormat.writes(document).as[JsObject]
  }

  implicit object BSONDocumentWrites
      extends JSONSerializationPack.Writer[BSONDocument] {
    def writes(bson: BSONDocument): JsObject =
      BSONFormats.BSONDocumentFormat.partialWrites(bson) match {
        case obj @ JsObject(_) => obj
        case js =>
          throw new ReactiveMongoPluginException(s"JSON object expected: $js")
      }
  }

  implicit object JsObjectDocumentWriter // Identity writer
      extends JSONSerializationPack.Writer[JsObject] {
    def writes(obj: JsObject): JSONSerializationPack.Document = obj
  }
}

sealed trait LowerImplicitBSONHandlers {
  import reactivemongo.bson.{ BSONElement, Producer }

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.jsWriter]]")
  implicit def jsWriter[A <: JsValue, B <: BSONValue] =
    PlayFormats.jsWriter[A, B]

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.JsFieldBSONElementProducer]]")
  implicit def JsFieldBSONElementProducer[T <: JsValue](jsField: (String, T)): Producer[BSONElement] = PlayFormats.JsFieldBSONElementProducer(jsField)

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONValueReads]]")
  implicit val BSONValueReads = PlayFormats.BSONValueReads

  @deprecated("0.11.9",
    "Use [[reactivemongo.play.json.BSONFormats.BSONValueWrites]]")
  implicit val BSONValueWrites = PlayFormats.BSONValueWrites
}
