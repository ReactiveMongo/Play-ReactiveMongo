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
  @deprecated("Use [[reactivemongo.play.json.readOpt]]", "0.11.9")
  val readOpt = json.readOpt
}

@deprecated(
  "Use [[reactivemongo.play.json.BSONFormats]]", "0.12.0")
object BSONFormats extends BSONFormats

/**
 * JSON Formats for BSONValues.
 */
sealed trait BSONFormats extends LowerImplicitBSONHandlers {
  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.PartialFormat]]", "0.11.9")
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

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONDoubleFormat]]", "0.11.9")
  implicit val BSONDoubleFormat: PartialFormat[BSONDouble] =
    PlayFormats.BSONDoubleFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONStringFormat]]", "0.11.9")
  implicit val BSONStringFormat: PartialFormat[BSONString] =
    PlayFormats.BSONStringFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONDocumentFormat]]", "0.11.9")
  class BSONDocumentFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PlayFormats.BSONDocumentFormat(toBSON, toJSON) with PartialFormat[BSONDocument] {
  }

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONDocumentFormat]]", "0.11.9")
  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONArrayFormat]]", "0.11.9")
  class BSONArrayFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PlayFormats.BSONArrayFormat(toBSON, toJSON) with PartialFormat[BSONArray] {
  }

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONArrayFormat]]", "0.11.9")
  implicit object BSONArrayFormat extends BSONArrayFormat(toBSON, toJSON)

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONObjectIDFormat]]", "0.11.9")
  implicit val BSONObjectIDFormat: PartialFormat[BSONObjectID] =
    PlayFormats.BSONObjectIDFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONBooleanFormat]]", "0.11.9")
  implicit val BSONBooleanFormat: PartialFormat[BSONBoolean] =
    PlayFormats.BSONBooleanFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONDateTimeFormat]]", "0.11.9")
  implicit val BSONDateTimeFormat: PartialFormat[BSONDateTime] =
    PlayFormats.BSONDateTimeFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONTimestampFormat]]", "0.11.9")
  implicit val BSONTimestampFormat: PartialFormat[BSONTimestamp] =
    PlayFormats.BSONTimestampFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONRegexFormat]]", "0.11.9")
  implicit val BSONRegexFormat: PartialFormat[BSONRegex] =
    PlayFormats.BSONRegexFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONNullFormat]]", "0.11.9")
  implicit val BSONNullFormat: PartialFormat[BSONNull.type] =
    PlayFormats.BSONNullFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONIntegerFormat]]", "0.11.9")
  implicit val BSONIntegerFormat: PartialFormat[BSONInteger] =
    PlayFormats.BSONIntegerFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONLongFormat]]", "0.11.9")
  implicit val BSONLongFormat: PartialFormat[BSONLong] =
    PlayFormats.BSONLongFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONBinaryFormat]]", "0.11.9")
  implicit val BSONBinaryFormat: PartialFormat[BSONBinary] =
    PlayFormats.BSONBinaryFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONSymbolFormat]]", "0.11.9")
  implicit val BSONSymbolFormat: PartialFormat[BSONSymbol] =
    PlayFormats.BSONSymbolFormat

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.numberReads]]", "0.11.9")
  val numberReads: PartialFunction[JsValue, JsResult[BSONValue]] =
    PlayFormats.numberReads

  @deprecated("Use [[reactivemongo.play.json.BSONFormats.toBSON]]", "0.11.9")
  def toBSON(json: JsValue): JsResult[BSONValue] = try {
    PlayFormats.toBSON(json)
  } catch {
    case je: JSONException =>
      throw new ReactiveMongoPluginException(je.getMessage)

    case ex: Throwable =>
      throw new ReactiveMongoPluginException(ex.getMessage, ex)
  }

  @deprecated("Use [[reactivemongo.play.json.BSONFormats.toJSON]]", "0.11.9")
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
  @deprecated("Use [[reactivemongo.play.json.Writers.JsPathMongo]]", "0.11.9")
  implicit class JsPathMongo(val jp: JsPath) extends AnyVal {
    def writemongo[A](implicit writer: Writes[A]): OWrites[A] =
      reactivemongo.play.json.Writers.JsPathMongo(jp).writemongo[A](writer)
  }
}

@deprecated("Use [[reactivemongo.play.json.JSONSerializationPack]]", "0.11.9")
object JSONSerializationPack extends reactivemongo.api.SerializationPack {
  import scala.util.{ Failure, Success, Try }

  import reactivemongo.play.json.{ JSONSerializationPack => PlayPack }

  import reactivemongo.bson.buffer.{
    ReadableBuffer,
    WritableBuffer
  }

  type Value = PlayPack.Value
  type ElementProducer = PlayPack.ElementProducer
  type Document = PlayPack.Document
  type Writer[A] = PlayPack.Writer[A]
  type Reader[A] = PlayPack.Reader[A]
  type NarrowValueReader[A] = Reads[A]
  type WidenValueReader[A] = Reads[A]

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.IdentityReader]]",
    "0.11.9")
  val IdentityReader: Reader[Document] = PlayPack.IdentityReader

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.IdentityWriter]]",
    "0.11.9")
  val IdentityWriter: Writer[Document] = PlayPack.IdentityWriter

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.serialize]]", "0.11.9")
  def serialize[A](a: A, writer: Writer[A]): Document =
    PlayPack.serialize[A](a, writer)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.deserialize]]",
    "0.11.9")
  def deserialize[A](document: Document, reader: Reader[A]): A =
    PlayPack.deserialize[A](document, reader)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.writeToBuffer]]",
    "0.11.9")
  def writeToBuffer(buffer: WritableBuffer, document: Document): WritableBuffer = PlayPack.writeToBuffer(buffer, document)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.readFromBuffer]]",
    "0.11.9")
  def readFromBuffer(buffer: ReadableBuffer): Document =
    PlayPack.readFromBuffer(buffer)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.writer]]",
    "0.11.9")
  def writer[A](f: A => Document): Writer[A] = PlayPack.writer[A](f)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.isEmpty]]",
    "0.11.9")
  def isEmpty(document: Document): Boolean = PlayPack.isEmpty(document)

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.widenReader]]",
    "0.11.10")
  def widenReader[T](r: NarrowValueReader[T]): WidenValueReader[T] = r

  @deprecated(
    "Use [[reactivemongo.play.json.JSONSerializationPack.readValue]]",
    "0.11.10")
  def readValue[A](value: Value, reader: WidenValueReader[A]): Try[A] =
    reader.reads(value) match {
      case err @ JsError(_) => Failure(new scala.RuntimeException(s"fails to reads the value: ${Json stringify value}; ${Json stringify JsError.toJson(err)}"))

      case JsSuccess(v, _)  => Success(v)
    }

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
@deprecated("Use [[reactivemongo.play.json.BSONFormats]]", "0.11.9")
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

  @deprecated("Use [[reactivemongo.play.json.BSONFormats.jsWriter]]", "0.11.9")
  implicit def jsWriter[A <: JsValue, B <: BSONValue] =
    PlayFormats.jsWriter[A, B]

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.JsFieldBSONElementProducer]]",
    "0.11.9")
  implicit def JsFieldBSONElementProducer[T <: JsValue](jsField: (String, T)): Producer[BSONElement] = PlayFormats.JsFieldBSONElementProducer(jsField)

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONValueReads]]",
    "0.11.9")
  implicit val BSONValueReads = PlayFormats.BSONValueReads

  @deprecated(
    "Use [[reactivemongo.play.json.BSONFormats.BSONValueWrites]]",
    "0.11.9")
  implicit val BSONValueWrites = PlayFormats.BSONValueWrites
}
