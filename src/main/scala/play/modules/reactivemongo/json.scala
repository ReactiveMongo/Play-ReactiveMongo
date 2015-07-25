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
import reactivemongo.bson.utils.Converters

import scala.math.BigDecimal.{
  double2bigDecimal,
  int2bigDecimal,
  long2bigDecimal
}

object `package` extends ImplicitBSONHandlers {
  object readOpt {
    implicit def optionReads[T](implicit r: Reads[T]): Reads[Option[T]] = Reads.optionWithNull[T]

    def apply[T](lookup: JsLookupResult)(implicit r: Reads[T]): JsResult[Option[T]] = lookup.toOption.fold[JsResult[Option[T]]](JsSuccess(None))(_.validate[Option[T]])
  }
}

object BSONFormats extends BSONFormats

/**
 * JSON Formats for BSONValues.
 */
sealed trait BSONFormats extends LowerImplicitBSONHandlers {
  trait PartialFormat[T <: BSONValue] extends Format[T] {
    def partialReads: PartialFunction[JsValue, JsResult[T]]
    def partialWrites: PartialFunction[BSONValue, JsValue]

    def writes(t: T): JsValue = partialWrites(t)
    def reads(json: JsValue) = partialReads.lift(json).getOrElse(JsError(s"unhandled json value: $json"))
  }

  implicit object BSONDoubleFormat extends PartialFormat[BSONDouble] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONDouble]] = {
      case JsNumber(f)        => JsSuccess(BSONDouble(f.toDouble))
      case DoubleValue(value) => JsSuccess(BSONDouble(value))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case double: BSONDouble => JsNumber(double.value)
    }

    private object DoubleValue {
      def unapply(obj: JsObject): Option[Double] =
        (obj \ "$double").asOpt[JsNumber].map(_.value.toDouble)
    }
  }

  implicit object BSONStringFormat extends PartialFormat[BSONString] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONString]] = {
      case JsString(str) => JsSuccess(BSONString(str))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case str: BSONString => JsString(str.value)
    }
  }

  class BSONDocumentFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PartialFormat[BSONDocument] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONDocument]] = {
      case obj: JsObject =>
        try {
          JsSuccess(bson(obj))
        } catch {
          case e: Throwable => JsError(e.getMessage)
        }
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case doc: BSONDocument => json(doc)
    }

    // UNSAFE - FOR INTERNAL USE
    private[json] def bson(obj: JsObject): BSONDocument = BSONDocument(
      obj.fields.map { tuple =>
        tuple._1 -> (toBSON(tuple._2) match {
          case JsSuccess(bson, _) => bson
          case JsError(err)       => throw new ReactiveMongoPluginException(err.toString)
        })
      })

    // UNSAFE - FOR INTERNAL USE
    private[json] def json(bson: BSONDocument): JsObject =
      JsObject(bson.elements.map(elem => elem._1 -> toJSON(elem._2)))
  }

  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)
  class BSONArrayFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PartialFormat[BSONArray] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONArray]] = {
      case arr: JsArray =>
        try {
          JsSuccess(BSONArray(arr.value.map { value =>
            toBSON(value) match {
              case JsSuccess(bson, _) => bson
              case JsError(err)       => throw new ReactiveMongoPluginException(err.toString)
            }
          }))
        } catch {
          case e: Throwable => JsError(e.getMessage)
        }
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case array: BSONArray => JsArray(array.values.map(toJSON))
    }
  }

  implicit object BSONArrayFormat extends BSONArrayFormat(toBSON, toJSON)

  implicit object BSONObjectIDFormat extends PartialFormat[BSONObjectID] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONObjectID]] = {
      case OidValue(oid) => JsSuccess(BSONObjectID(oid))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case oid: BSONObjectID => Json.obj("$oid" -> oid.stringify)
    }

    private object OidValue {
      def unapply(obj: JsObject): Option[String] =
        if (obj.fields.size != 1) None else (obj \ "$oid").asOpt[String]
    }
  }

  implicit object BSONBooleanFormat extends PartialFormat[BSONBoolean] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONBoolean]] = {
      case JsBoolean(v) => JsSuccess(BSONBoolean(v))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case boolean: BSONBoolean => JsBoolean(boolean.value)
    }
  }

  implicit object BSONDateTimeFormat extends PartialFormat[BSONDateTime] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONDateTime]] = {
      case DateValue(value) => JsSuccess(BSONDateTime(value))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case dt: BSONDateTime => Json.obj("$date" -> dt.value)
    }

    private object DateValue {
      def unapply(obj: JsObject): Option[Long] = (obj \ "$date").asOpt[Long]
    }
  }

  implicit object BSONTimestampFormat extends PartialFormat[BSONTimestamp] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONTimestamp]] = {
      case TimeValue((time, i)) => JsSuccess(BSONTimestamp((time << 32) ^ i))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case ts: BSONTimestamp => Json.obj(
        "$time" -> (ts.value >>> 32), "$i" -> ts.value.toInt)
    }

    private object TimeValue {
      def unapply(obj: JsObject): Option[(Long, Int)] = for {
        time <- (obj \ "$time").asOpt[Long]
        i <- (obj \ "$i").asOpt[Int]
      } yield (time, i)
    }
  }

  implicit object BSONRegexFormat extends PartialFormat[BSONRegex] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONRegex]] = {
      case js: JsObject if js.values.size == 1 && js.fields.head._1 == "$regex" =>
        js.fields.head._2.asOpt[String].
          map(rx => JsSuccess(BSONRegex(rx, ""))).
          getOrElse(JsError(__ \ "$regex", "string expected"))
      case js: JsObject if js.value.size == 2 && js.value.exists(_._1 == "$regex") && js.value.exists(_._1 == "$options") =>
        val rx = (js \ "$regex").asOpt[String]
        val opts = (js \ "$options").asOpt[String]
        (rx, opts) match {
          case (Some(rx), Some(opts)) => JsSuccess(BSONRegex(rx, opts))
          case (None, Some(_))        => JsError(__ \ "$regex", "string expected")
          case (Some(_), None)        => JsError(__ \ "$options", "string expected")
          case _                      => JsError(__ \ "$regex", "string expected") ++ JsError(__ \ "$options", "string expected")
        }
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case rx: BSONRegex =>
        if (rx.flags.isEmpty)
          Json.obj("$regex" -> rx.value)
        else Json.obj("$regex" -> rx.value, "$options" -> rx.flags)
    }
  }

  implicit object BSONNullFormat extends PartialFormat[BSONNull.type] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONNull.type]] = {
      case JsNull => JsSuccess(BSONNull)
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case BSONNull => JsNull
    }
  }

  implicit object BSONIntegerFormat extends PartialFormat[BSONInteger] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONInteger]] = {
      case JsNumber(i)     => JsSuccess(BSONInteger(i.toInt))
      case IntValue(value) => JsSuccess(BSONInteger(value))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case int: BSONInteger => JsNumber(int.value)
    }

    private object IntValue {
      def unapply(obj: JsObject): Option[Int] =
        (obj \ "$int").asOpt[JsNumber].map(_.value.toInt)
    }
  }

  implicit object BSONLongFormat extends PartialFormat[BSONLong] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONLong]] = {
      case JsNumber(long)   => JsSuccess(BSONLong(long.toLong))
      case LongValue(value) => JsSuccess(BSONLong(value))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case long: BSONLong => JsNumber(long.value)
    }

    private object LongValue {
      def unapply(obj: JsObject): Option[Long] =
        (obj \ "$long").asOpt[JsNumber].map(_.value.toLong)
    }
  }

  implicit object BSONBinaryFormat extends PartialFormat[BSONBinary] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONBinary]] = {
      case JsString(str) => try {
        JsSuccess(BSONBinary(Converters.str2Hex(str), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => JsError(s"error deserializing hex ${e.getMessage}")
      }
      case obj: JsObject if obj.fields.exists {
        case (str, _: JsString) if str == "$binary" => true
        case _                                      => false
      } => try {
        JsSuccess(BSONBinary(Converters.str2Hex((obj \ "$binary").as[String]), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => JsError(s"error deserializing hex ${e.getMessage}")
      }
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case binary: BSONBinary =>
        val remaining = binary.value.readable()
        Json.obj(
          "$binary" -> Converters.hex2Str(binary.value.slice(remaining).readArray(remaining)),
          "$type" -> Converters.hex2Str(Array(binary.subtype.value.toByte)))
    }
  }

  implicit object BSONSymbolFormat extends PartialFormat[BSONSymbol] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONSymbol]] = {
      case SymbolValue(value) => JsSuccess(BSONSymbol(value))
    }

    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case BSONSymbol(s) => Json.obj("$symbol" -> s)
    }

    private object SymbolValue {
      def unapply(obj: JsObject): Option[String] =
        if (obj.fields.size != 1) None else (obj \ "$symbol").asOpt[String]
    }
  }

  val numberReads: PartialFunction[JsValue, JsResult[BSONValue]] = {
    case JsNumber(n) if !n.ulp.isWhole => JsSuccess(BSONDouble(n.toDouble))
    case JsNumber(n) if n.isValidInt   => JsSuccess(BSONInteger(n.toInt))
    case JsNumber(n) if n.isValidLong  => JsSuccess(BSONLong(n.toLong))
  }

  def toBSON(json: JsValue): JsResult[BSONValue] =
    BSONStringFormat.partialReads.
      orElse(BSONObjectIDFormat.partialReads).
      orElse(BSONDateTimeFormat.partialReads).
      orElse(BSONTimestampFormat.partialReads).
      orElse(BSONBinaryFormat.partialReads).
      orElse(BSONRegexFormat.partialReads).
      orElse(numberReads).
      orElse(BSONBooleanFormat.partialReads).
      orElse(BSONNullFormat.partialReads).
      orElse(BSONSymbolFormat.partialReads).
      orElse(BSONArrayFormat.partialReads).
      orElse(BSONDocumentFormat.partialReads).
      lift(json).getOrElse(JsError(s"unhandled json value: $json"))

  def toJSON(bson: BSONValue): JsValue = BSONObjectIDFormat.partialWrites.
    orElse(BSONDateTimeFormat.partialWrites).
    orElse(BSONTimestampFormat.partialWrites).
    orElse(BSONBinaryFormat.partialWrites).
    orElse(BSONRegexFormat.partialWrites).
    orElse(BSONDoubleFormat.partialWrites).
    orElse(BSONIntegerFormat.partialWrites).
    orElse(BSONLongFormat.partialWrites).
    orElse(BSONBooleanFormat.partialWrites).
    orElse(BSONNullFormat.partialWrites).
    orElse(BSONStringFormat.partialWrites).
    orElse(BSONSymbolFormat.partialWrites).
    orElse(BSONArrayFormat.partialWrites).
    orElse(BSONDocumentFormat.partialWrites).
    lift(bson).getOrElse(throw new ReactiveMongoPluginException(s"Unhandled json value: $bson"))
}

object Writers {
  implicit class JsPathMongo(val jp: JsPath) extends AnyVal {
    def writemongo[A](implicit writer: Writes[A]): OWrites[A] = {
      OWrites[A] { (o: A) =>
        val newPath = jp.path.flatMap {
          case e: KeyPathNode     => Some(e.key)
          case e: RecursiveSearch => Some(s"$$.${e.key}")
          case e: IdxPathNode     => Some(s"${e.idx}")
        }.mkString(".")

        val orig = writer.writes(o)
        orig match {
          case JsObject(e) =>
            JsObject(e.flatMap {
              case (k, v) => Seq(s"${newPath}.$k" -> v)
            })
          case e: JsValue => JsObject(Seq(newPath -> e))
        }
      }
    }
  }
}

object JSONSerializationPack extends reactivemongo.api.SerializationPack {
  import reactivemongo.bson.buffer.{
    DefaultBufferHandler,
    ReadableBuffer,
    WritableBuffer
  }

  type Value = JsValue
  type ElementProducer = (String, Json.JsValueWrapper)
  type Document = JsObject
  type Writer[A] = OWrites[A]
  type Reader[A] = Reads[A]

  object IdentityReader extends Reader[Document] {
    def reads(js: JsValue): JsResult[Document] = js match {
      case o: JsObject => JsSuccess(o)
      case v           => JsError(s"object is expected: $v")
    }
  }

  object IdentityWriter extends Writer[Document] {
    def writes(document: Document): Document = document
  }

  def serialize[A](a: A, writer: Writer[A]): Document = writer.writes(a)

  def deserialize[A](document: Document, reader: Reader[A]): A =
    reader.reads(document) match {
      case JsError(msg)    => sys.error(msg mkString ", ")
      case JsSuccess(v, _) => v
    }

  def writeToBuffer(buffer: WritableBuffer, document: Document): WritableBuffer = {
    BSONDocument.write(BSONFormats.toBSON(document).flatMap[BSONDocument] {
      case d: BSONDocument => JsSuccess(d)
      case v               => JsError(s"document is expected: $v")
    }.get, buffer)
    buffer
  }

  def readFromBuffer(buffer: ReadableBuffer): Document =
    BSONFormats.toJSON(BSONDocument.read(buffer)).as[Document]

  def writer[A](f: A => Document): Writer[A] = new OWrites[A] {
    def writes(input: A): Document = f(input)
  }

  def isEmpty(document: Document): Boolean = document.values.isEmpty
}

import play.api.libs.json.{ JsObject, JsValue }
import reactivemongo.bson.{
  BSONDocument,
  BSONDocumentReader,
  BSONDocumentWriter
}

object ImplicitBSONHandlers extends ImplicitBSONHandlers

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
sealed trait ImplicitBSONHandlers extends BSONFormats {
  implicit object JsObjectWriter extends BSONDocumentWriter[JsObject] {
    def write(obj: JsObject): BSONDocument =
      BSONFormats.BSONDocumentFormat.bson(obj)
  }

  implicit object JsObjectReader extends BSONDocumentReader[JsObject] {
    def read(document: BSONDocument) =
      BSONFormats.BSONDocumentFormat.writes(document).as[JsObject]
  }

  implicit object BSONDocumentWrites
      extends JSONSerializationPack.Writer[BSONDocument] {
    def writes(bson: BSONDocument): JsObject =
      BSONFormats.BSONDocumentFormat.json(bson)
  }

  implicit object JsObjectDocumentWriter // Identity writer
      extends JSONSerializationPack.Writer[JsObject] {
    def writes(obj: JsObject): JSONSerializationPack.Document = obj
  }
}

sealed trait LowerImplicitBSONHandlers {
  import reactivemongo.bson.{ BSONElement, Producer }

  implicit def jsWriter[A <: JsValue, B <: BSONValue] = new BSONWriter[A, B] {
    def write(js: A): B = BSONFormats.toBSON(js).get.asInstanceOf[B]
  }

  implicit def JsFieldBSONElementProducer[T <: JsValue](jsField: (String, T)): Producer[BSONElement] = Producer.nameValue2Producer(jsField)

  implicit object BSONValueReads extends Reads[BSONValue] {
    def reads(js: JsValue) = BSONFormats.toBSON(js)
  }

  implicit object BSONValueWrites extends Writes[BSONValue] {
    def writes(bson: BSONValue) = BSONFormats.toJSON(bson)
  }
}
