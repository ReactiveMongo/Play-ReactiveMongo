/*
 * Copyright 2012 Pascal Voitot
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
package play.modules.reactivemongo

import org.jboss.netty.buffer._
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.utils.Converters
import scala.util.{ Failure, Success, Try }

trait LowerReactiveBSONImplicits { self: ReactiveBSONImplicits =>
  implicit object JsValueWriter extends BSONDocumentWriter[JsValue] {
    def write(jsValue: JsValue) = jsValue match {
      case obj: JsObject => self.JsObjectWriter.write(obj)
      case _ => throw new UnsupportedOperationException(
        s"JSON value of type ${jsValue.getClass().getCanonicalName()} can not be converted to a document")
    }
  }
  implicit object JsValueReader extends BSONDocumentReader[JsValue] {
    def read(document: BSONDocument) = self.JsObjectReader.read(document)
  }
}

trait ReactiveBSONImplicits extends LowerReactiveBSONImplicits {
  implicit object JsObjectWriter extends BSONDocumentWriter[JsObject] {
    private val specials: PartialFunction[JsValue, BSONValue] = {
      case obj: JsObject if obj.value.headOption.filter(s => s._2.isInstanceOf[JsString] && s._1 == "$oid").isDefined =>
        BSONObjectID(obj.value.head._2.as[String])
      case JsObject(("$oid", JsString(v)) :: _)    => BSONObjectID(Converters.str2Hex(v))
      case JsObject(("$date", JsNumber(v)) :: _)   => BSONDateTime(v.toLong)
      case JsObject(("$int", JsNumber(v)) :: _)    => BSONInteger(v.toInt)
      case JsObject(("$long", JsNumber(v)) :: _)   => BSONLong(v.toLong)
      case JsObject(("$double", JsNumber(v)) :: _) => BSONDouble(v.toDouble)
    }

    def write(obj: JsObject): BSONDocument = {
      BSONDocument(obj.fields.map { tuple =>
        tuple._1 -> specials.lift(matchable(tuple._2)).getOrElse(MongoJSONHelpers.toBSON(tuple._2))
      }.toStream)
    }

    private def matchable(obj: JsValue): JsValue = obj match {
      case JsObject(fields) => JsObject(fields.toList)
      case other            => other
    }
  }

  implicit object JsObjectReader extends BSONDocumentReader[JsObject] {
    def read(document: BSONDocument) = {
      JsObject(document.elements.map { element =>
        element._1 -> MongoJSONHelpers.toJSON(element._2)
      })
    }
  }
}

trait JSONLibraryImplicits {
  import ReactiveBSONImplicits._

  implicit object BSONDocumentFormat extends Format[BSONDocument] {
    def writes(doc: BSONDocument) = JsObjectReader.read(doc)
    def reads(js: JsValue) = js match {
      case obj: JsObject => JsSuccess(JsObjectWriter.write(obj))
      case _             => JsError("expected a jsobject to convert to a BSONDocument")
    }
  }

  implicit object BSONArrayFormat extends Format[BSONArray] {
    def writes(bson: BSONArray) = MongoJSONHelpers.toJSON(bson)
    def reads(js: JsValue) = js match {
      case arr: JsArray => JsSuccess(MongoJSONHelpers.toBSON(arr).asInstanceOf[BSONArray])
      case _            => JsError("expected a JsArray to convert to a BSONArray")
    }
  }
}

object ReactiveBSONImplicits extends ReactiveBSONImplicits
object JSONLibraryImplicits extends JSONLibraryImplicits
object Implicits extends ReactiveBSONImplicits with JSONLibraryImplicits

trait MongoJSONHelpers {
  def Date(d: java.util.Date) = Json.obj("$date" -> d.getTime)
  def Date(l: Long) = Json.obj("$date" -> l)

  def ObjectId(s: String) = Json.obj("$oid" -> s)
  def ObjectId(s: Array[Byte]) = Json.obj("$oid" -> Converters.hex2Str(s))

  def RegEx(regex: String, options: String = "") = Json.obj("$regex" -> regex, "$options" -> options)
  def LessThan(obj: JsObject) = Json.obj("$lt" -> obj)

  def toJSON(bson: BSONValue): JsValue = bson match {
    case BSONDouble(value) => JsNumber(value)
    case BSONString(value) => JsString(value)
    case doc: BSONDocument => Implicits.JsObjectReader.read(doc)
    case array: BSONArray => JsArray(array.values.map {
      (value =>
        toJSON(value))
    })
    case oid @ BSONObjectID(value) => Json.obj("$oid" -> oid.stringify)
    case BSONBoolean(value)        => JsBoolean(value)
    case BSONDateTime(value)       => Json.obj("$date" -> value)
    case BSONTimestamp(value)      => Json.obj("$time" -> value.toInt, "i" -> (value >>> 4))
    case BSONRegex(value, flags)   => Json.obj("$regex" -> value, "$options" -> flags)
    case BSONNull                  => JsNull
    case BSONUndefined             => JsUndefined("")
    case BSONInteger(value)        => JsNumber(value)
    case BSONLong(value)           => JsNumber(value)
    case BSONBinary(value, subType) =>
      val arr = new Array[Byte](value.readable)
      value.readBytes(arr)
      Json.obj(
        "$binary" -> Converters.hex2Str(arr),
        "$type" -> Converters.hex2Str(Array(subType.value.toByte)))
    case BSONDBPointer(value, id) => Json.obj("$ref" -> value, "$id" -> Converters.hex2Str(id))
    // NOT STANDARD AT ALL WITH JSON and MONGO
    case BSONJavaScript(value)    => Json.obj("$js" -> value)
    case BSONSymbol(value)        => Json.obj("$sym" -> value)
    case BSONJavaScriptWS(value)  => Json.obj("$jsws" -> value)
    case BSONMinKey               => Json.obj("$minkey" -> 0)
    case BSONMaxKey               => Json.obj("$maxkey" -> 0)
  }

  def toBSON(value: JsValue): BSONValue = value match {
    case s: JsString => BSONString(s.value)
    case i: JsNumber => BSONDouble(i.value.toDouble)
    case o: JsObject => Implicits.JsObjectWriter.write(o)
    case a: JsArray =>
      BSONArray(a.value.map { elem =>
        toBSON(elem)
      }.toStream)
    case b: JsBoolean   => BSONBoolean(b.value)
    case JsNull         => BSONNull
    case u: JsUndefined => BSONUndefined
  }
}

object MongoJSONHelpers extends MongoJSONHelpers
