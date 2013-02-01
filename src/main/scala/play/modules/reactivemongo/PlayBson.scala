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
import reactivemongo.bson.handlers._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.utils.Converters

object PlayBsonImplicits extends PlayBsonImplicits

trait BSONBuilder[T, U <: AppendableBSONStructure[_]] {
  def write(t: T, bson: U): U
}

object MongoHelpers {
  def Date(d: java.util.Date) = Json.obj("$date" -> d.getTime)
  def Date(l: Long) = Json.obj("$date" -> l)

  def ObjectId(s: String) = Json.obj("$oid" -> s)
  def ObjectId(s: Array[Byte]) = Json.obj("$oid" -> Converters.hex2Str(s))

  def RegEx(regex: String, options: String = "") = Json.obj("$regex" -> regex, "$options" -> options)
  def LessThan(obj: JsObject) = Json.obj("$lt" -> obj)

}

trait PlayBsonImplicits {
  implicit object JsObjectBSONBuilder extends BSONBuilder[JsObject, AppendableBSONDocument] {
    def write(o: JsObject, bson: AppendableBSONDocument) = {
      bson.append(o.fields.map { t => val b = _toBson(t); b.name -> b.value }: _*)
    }
  }

  implicit object JsArrayBSONBuilder extends BSONBuilder[JsArray, AppendableBSONArray] {
    def write(o: JsArray, bson: AppendableBSONArray) = {
      bson.append(o.value.zipWithIndex.map { t: (JsValue, Int) =>
        _toBson(t._2.toString, t._1).value
      }: _*)
    }
  }

  def write2BSON[T](t: T, bson: AppendableBSONDocument)(implicit builder: BSONBuilder[T, AppendableBSONDocument]): AppendableBSONDocument = {
    builder.write(t, bson)
  }

  def _manageSpecials(t: (String, JsObject)): Either[(String, JsObject), BSONElement] = {
    if (t._2.fields.length > 0) {
      t._2.fields(0) match {
        case ("$oid", JsString(v))    => Right(DefaultBSONElement(t._1, BSONObjectID(Converters.str2Hex(v))))
        case ("$date", JsNumber(v))   => Right(DefaultBSONElement(t._1, BSONDateTime(v.toLong)))
        case ("$int", JsNumber(v))    => Right(DefaultBSONElement(t._1, BSONInteger(v.toInt)))
        case ("$long", JsNumber(v))   => Right(DefaultBSONElement(t._1, BSONLong(v.toLong)))
        case ("$double", JsNumber(v)) => Right(DefaultBSONElement(t._1, BSONDouble(v.toDouble)))
        case _                        => Left(t)
      }
    }
    else Left(t)
  }

  def _toBson(t: (String, JsValue)): BSONElement = {
    t._2 match {
      case s: JsString => DefaultBSONElement(t._1, BSONString(s.value))
      case i: JsNumber => DefaultBSONElement(t._1, BSONDouble(i.value.toDouble))
      case o: JsObject =>
        _manageSpecials((t._1, o)).fold(
          normal => DefaultBSONElement(normal._1, write2BSON(normal._2, BSONDocument())),
          special => special)
      case a: JsArray =>
        DefaultBSONElement(t._1, JsArrayBSONBuilder.write(a, BSONArray()))
      case b: JsBoolean   => DefaultBSONElement(t._1, BSONBoolean(b.value))
      case JsNull         => DefaultBSONElement(t._1, BSONNull)
      case u: JsUndefined => DefaultBSONElement(t._1, BSONUndefined)
    }
  }

  object JsObjectWriter extends RawBSONWriter[JsObject] {
    def write(doc: JsObject): ChannelBuffer = {
      JsObjectBSONBuilder.write(doc, BSONDocument()).makeBuffer
    }
  }

  object JsArrayWriter extends RawBSONWriter[JsArray] {
    def write(doc: JsArray): ChannelBuffer = {
      JsArrayBSONBuilder.write(doc, BSONArray()).makeBuffer
    }
  }

  implicit object JsValueWriter extends RawBSONWriter[JsValue] {
    def write(doc: JsValue): ChannelBuffer = {
      doc match {
        case o: JsObject => JsObjectWriter.write(o)
        case a: JsArray  => JsArrayWriter.write(a)
        case _           => throw new RuntimeException("JsValue can only JsObject/JsArray")
      }
    }
  }

  def toTuple(e: BSONElement): (String, JsValue) = e.name -> (e.value match {
    case BSONDouble(value)                    => JsNumber(value)
    case BSONString(value)                    => JsString(value)
    case traversable: TraversableBSONDocument => JsObjectReader.read(traversable.toBuffer)
    case doc: AppendableBSONDocument          => JsObjectReader.read(doc.toTraversable.toBuffer)
    case array: TraversableBSONArray => {
      val elems = array.iterator.foldLeft(List.empty[JsValue]) { (acc: List[JsValue], e: BSONElement) => toTuple(e)._2 :: acc }
      Json.arr(elems.reverse)
    }
    case array: AppendableBSONArray => JsArrayReader.read(array)
    case oid @ BSONObjectID(value)  => Json.obj("$oid" -> oid.stringify)
    case BSONBoolean(value)         => JsBoolean(value)
    case BSONDateTime(value)        => Json.obj("$date" -> value)
    case BSONTimestamp(value)       => Json.obj("$time" -> value.toInt, "i" -> (value >>> 4))
    case BSONRegex(value, flags)    => Json.obj("$regex" -> value, "$options" -> flags)
    case BSONNull                   => JsNull
    case BSONUndefined              => JsUndefined("")
    case BSONInteger(value)         => JsNumber(value)
    case BSONLong(value)            => JsNumber(value)
    case BSONBinary(value, subType) =>
      val arr = new Array[Byte](value.readableBytes())
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
  })

  object JsArrayReader {
    def read(array: BSONArray): JsArray = {
      val it = array.toTraversable.iterator

      val elems = it.foldLeft(List.empty[JsValue]) { (acc: List[JsValue], e: BSONElement) => toTuple(e)._2 :: acc }
      Json.arr(elems.reverse)
    }
  }

  object JsObjectReader extends BSONReader[JsObject] {
    def fromBSON(doc: BSONDocument): JsObject = {
      val elems = doc.toTraversable.iterator.foldLeft(List[(String, JsValue)]()) { (acc, e) => toTuple(e) :: acc }
      JsObject(elems.reverse)
    }
  }

  implicit object JsValueReader extends BSONReader[JsValue] {
    def fromBSON(doc: BSONDocument): JsValue = JsObjectReader.fromBSON(doc)
  }
}
