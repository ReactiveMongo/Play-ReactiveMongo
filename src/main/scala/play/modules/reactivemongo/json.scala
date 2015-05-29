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

import play.modules.reactivemongo.{ NonNumericHandling, ReactiveMongoPlugin }
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters
import play.api.libs.json._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import scala.math.BigDecimal.long2bigDecimal

/**
 * JSON Formats for BSONValues.
 */
object BSONFormats {
  trait PartialFormat[T <: BSONValue] extends Format[T] {
    def partialReads: PartialFunction[JsValue, JsResult[T]]
    def partialWrites: PartialFunction[BSONValue, JsValue]

    def writes(t: T): JsValue = partialWrites(t)
    def reads(json: JsValue) = partialReads.lift(json).getOrElse(JsError("unhandled json value"))
  }

  implicit object BSONDoubleFormat extends PartialFormat[BSONDouble] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONDouble]] = {
      case JsNumber(f)                               => JsSuccess(BSONDouble(f.toDouble))
      case JsObject(("$double", JsNumber(v)) +: Nil) => JsSuccess(BSONDouble(v.toDouble))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case double: BSONDouble => try {
        JsNumber(double.value)
      } catch {
        case e: NumberFormatException => ReactiveMongoPlugin.nonNumericHandling match {
          case NonNumericHandling.AsNull   => JsNull
          case NonNumericHandling.AsString => JsString(double.value.toString)
          case _                           => throw e
        }
      }
    }
  }
  implicit object BSONStringFormat extends PartialFormat[BSONString] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONString]] = {
      case JsString(str) => JsSuccess(BSONString(str))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case str: BSONString => JsString(str.value)
    }
  }
  class BSONDocumentFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PartialFormat[BSONDocument] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONDocument]] = {
      case obj: JsObject =>
        try {
          JsSuccess(BSONDocument(obj.fields.map { tuple =>
            tuple._1 -> (toBSON(tuple._2) match {
              case JsSuccess(bson, _) => bson
              case JsError(err)       => throw new RuntimeException(err.toString)
            })
          }))
        } catch {
          case e: Throwable => JsError(e.getMessage())
        }
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case doc: BSONDocument => new JsObject(doc.elements.map { elem =>
        elem._1 -> toJSON(elem._2)
      })
    }
  }
  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)
  class BSONArrayFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PartialFormat[BSONArray] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONArray]] = {
      case arr: JsArray =>
        try {
          JsSuccess(BSONArray(arr.value.map { value =>
            toBSON(value) match {
              case JsSuccess(bson, _) => bson
              case JsError(err)       => throw new RuntimeException(err.toString)
            }
          }))
        } catch {
          case e: Throwable => JsError(e.getMessage())
        }
    }
    def partialWrites: PartialFunction[BSONValue, JsValue] = {
      case array: BSONArray => {
        JsArray(array.values.map { value =>
          toJSON(value)
        })
      }
    }
  }
  implicit object BSONArrayFormat extends BSONArrayFormat(toBSON, toJSON)
  implicit object BSONObjectIDFormat extends PartialFormat[BSONObjectID] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONObjectID]] = {
      case JsObject(("$oid", JsString(v)) +: Nil) => JsSuccess(BSONObjectID(v))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case oid: BSONObjectID => Json.obj("$oid" -> oid.stringify)
    }
  }
  implicit object BSONBooleanFormat extends PartialFormat[BSONBoolean] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONBoolean]] = {
      case JsBoolean(v) => JsSuccess(BSONBoolean(v))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case boolean: BSONBoolean => JsBoolean(boolean.value)
    }
  }
  implicit object BSONDateTimeFormat extends PartialFormat[BSONDateTime] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONDateTime]] = {
      case JsObject(("$date", JsNumber(v)) +: Nil) => JsSuccess(BSONDateTime(v.toLong))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case dt: BSONDateTime => Json.obj("$date" -> dt.value)
    }
  }
  implicit object BSONTimestampFormat extends PartialFormat[BSONTimestamp] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONTimestamp]] = {
      case JsObject(("$time", JsNumber(v)) +: Nil) => JsSuccess(BSONTimestamp(v.toLong))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case ts: BSONTimestamp => Json.obj("$time" -> ts.value.toInt, "i" -> (ts.value >>> 4))
    }
  }
  implicit object BSONRegexFormat extends PartialFormat[BSONRegex] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONRegex]] = {
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
        if (rx.flags.isEmpty())
          Json.obj("$regex" -> rx.value)
        else Json.obj("$regex" -> rx.value, "$options" -> rx.flags)
    }
  }
  implicit object BSONNullFormat extends PartialFormat[BSONNull.type] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONNull.type]] = {
      case JsNull => JsSuccess(BSONNull)
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case BSONNull => JsNull
    }
  }
  implicit object BSONUndefinedFormat extends PartialFormat[BSONUndefined.type] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONUndefined.type]] = {
      case _: JsUndefined => JsSuccess(BSONUndefined)
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case BSONUndefined => JsUndefined("")
    }
  }
  implicit object BSONIntegerFormat extends PartialFormat[BSONInteger] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONInteger]] = {
      case JsObject(("$int", JsNumber(i)) +: Nil) => JsSuccess(BSONInteger(i.toInt))
      case JsNumber(i)                            => JsSuccess(BSONInteger(i.toInt))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case int: BSONInteger => JsNumber(int.value)
    }
  }
  implicit object BSONLongFormat extends PartialFormat[BSONLong] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONLong]] = {
      case JsObject(("$long", JsNumber(long)) +: Nil) => JsSuccess(BSONLong(long.toLong))
      case JsNumber(long)                             => JsSuccess(BSONLong(long.toLong))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case long: BSONLong => JsNumber(long.value)
    }
  }
  implicit object BSONBinaryFormat extends PartialFormat[BSONBinary] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONBinary]] = {
      case JsString(str) => try {
        JsSuccess(BSONBinary(Converters.str2Hex(str), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => JsError(s"error deserializing hex ${e.getMessage()}")
      }
      case obj: JsObject if obj.fields.exists {
        case (str, _: JsString) if str == "$binary" => true
        case _                                      => false
      } => try {
        JsSuccess(BSONBinary(Converters.str2Hex((obj \ "$binary").as[String]), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => JsError(s"error deserializing hex ${e.getMessage()}")
      }
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case binary: BSONBinary => {
        val remaining = binary.value.readable
        Json.obj(
          "$binary" -> Converters.hex2Str(binary.value.slice(remaining).readArray(remaining)),
          "$type" -> Converters.hex2Str(Array(binary.subtype.value.toByte)))
      }
    }
  }
  implicit object BSONSymbolFormat extends PartialFormat[BSONSymbol] {
    def partialReads: PartialFunction[JsValue, JsResult[BSONSymbol]] = {
      case JsObject(("$symbol", JsString(v)) +: Nil) => JsSuccess(BSONSymbol(v))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case BSONSymbol(s) => Json.obj("$symbol" -> s)
    }
  }

  def toBSON(json: JsValue): JsResult[BSONValue] = {
    BSONStringFormat.partialReads.
      orElse(BSONObjectIDFormat.partialReads).
      orElse(BSONDateTimeFormat.partialReads).
      orElse(BSONTimestampFormat.partialReads).
      orElse(BSONBinaryFormat.partialReads).
      orElse(BSONRegexFormat.partialReads).
      orElse(BSONDoubleFormat.partialReads).
      orElse(BSONIntegerFormat.partialReads).
      orElse(BSONLongFormat.partialReads).
      orElse(BSONBooleanFormat.partialReads).
      orElse(BSONNullFormat.partialReads).
      orElse(BSONUndefinedFormat.partialReads).
      orElse(BSONSymbolFormat.partialReads).
      orElse(BSONArrayFormat.partialReads).
      orElse(BSONDocumentFormat.partialReads).
      lift(json).getOrElse(JsError(s"unhandled json value: $json"))
  }

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
    orElse(BSONUndefinedFormat.partialWrites).
    orElse(BSONStringFormat.partialWrites).
    orElse(BSONSymbolFormat.partialWrites).
    orElse(BSONArrayFormat.partialWrites).
    orElse(BSONDocumentFormat.partialWrites).
    lift(bson).getOrElse(throw new RuntimeException(s"unhandled json value: $bson"))
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
              case (k, v) => Seq(s"${newPath}.${k}" -> v)
            })
          case e: JsValue => JsObject(Seq(newPath -> e))
        }
      }
    }
  }
}

