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

package play.modules.mongodb

import play.api.libs.json._
import org.asyncmongo.bson._
import org.asyncmongo.handlers._
import org.jboss.netty.buffer._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.utils.Converters

object PlayBsonImplicits extends PlayBsonImplicits

trait BSONBuilder[T] {
  def write(t: T, bson: Bson): Bson
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

  implicit object JsObjectBSONBuilder extends BSONBuilder[JsObject] {
    def write(o: JsObject, bson: Bson) = {
      o.fields.foreach{ t:(String, JsValue) => val b = _toBson(t); println(b); bson.write(b) }
      bson
    }
  }

  implicit object JsArrayBSONBuilder extends BSONBuilder[JsArray] {
    def write(o: JsArray, bson: Bson) = {
      o.value.zipWithIndex.map{ t:(JsValue, Int) => 
        (t._2.toString, t._1) }.foreach{ t:(String, JsValue) => val b = _toBson(t); println(b); bson.write(b)
      }
      bson
    }
  }


  def write2BSON[T](t: T, bson: Bson)(implicit builder:BSONBuilder[T]): Bson = {
    builder.write(t, bson)
  }

  def _manageSpecials(t: (String, JsObject)): Either[(String, JsObject), BSONElement] = {
    if(t._2.fields.length > 0) {
      t._2.fields(0) match {
        case ("$oid", JsString(v)) => Right(DefaultBSONElement(t._1, BSONObjectID(Converters.str2Hex(v))))
        case ("$date", JsNumber(v)) => Right(DefaultBSONElement(t._1, BSONDateTime(v.toLong)))
        case (k, _) if(Seq("$gt", "$lt").contains(k)) => Left(t)
        case (k, _) if(k.startsWith("$")) => throw new RuntimeException("unmanaged special %s".format(k))
        case _ => Left(t)
      }
    } else Left(t)
  }

  def _toBson(t: (String, JsValue)): BSONElement = {
    t._2 match {
      case s: JsString => DefaultBSONElement(t._1, BSONString(s.value))
      case i: JsNumber => DefaultBSONElement(t._1, BSONDouble(i.value.toDouble))
      case o: JsObject =>         
        _manageSpecials((t._1, o)).fold (
          normal => DefaultBSONElement(normal._1, BSONDocument(write2BSON(normal._2, new Bson()).getBuffer)),
          special => special
        )
        
      case a: JsArray => 
        val _bson = new Bson()
        JsArrayBSONBuilder.write(a, _bson)
        DefaultBSONElement(t._1, BSONArray(_bson.getBuffer))
      case b: JsBoolean => DefaultBSONElement(t._1, BSONBoolean(b.value))
      case JsNull => DefaultBSONElement(t._1, BSONNull)
      case u: JsUndefined => DefaultBSONElement(t._1, BSONUndefined)
    }
  }

  implicit object JsObjectWriter extends BSONWriter[JsObject] {
    def write(doc: JsObject): ChannelBuffer = {
      val bson = new Bson()
      JsObjectBSONBuilder.write(doc, bson)
      bson.getBuffer
    }
  }

  implicit object JsArrayWriter extends BSONWriter[JsArray] {
    def write(doc: JsArray): ChannelBuffer = {
      val bson = new Bson()
      JsArrayBSONBuilder.write(doc, bson)
      bson.getBuffer
    }
  }

  implicit object JsValueWriter extends BSONWriter[JsValue] {
    def write(doc: JsValue): ChannelBuffer = {
      doc match {
        case o: JsObject => JsObjectWriter.write(o)
        case a: JsArray => JsArrayWriter.write(a)
        case _ => throw new RuntimeException("JsValue can only JsObject/JsArray")
      }
    }
  }

  def toTuple(e: BSONElement): (String, JsValue) = e.name -> (e.value match {
      case BSONDouble(value) => JsNumber(value)
      case BSONString(value) => JsString(value)
      case BSONDocument(value) => JsObjectReader.read(value)
      case BSONArray(value) => JsArrayReader.read(value)
      case oid @ BSONObjectID(value) => Json.obj( "$oid" -> oid.stringify )
      case BSONBoolean(value) => JsBoolean(value)
      case BSONDateTime(value) => Json.obj("$date" -> value)
      case BSONTimestamp(value) => Json.obj("$time" -> value.toInt, "i" -> (value >>> 4) )
      case BSONRegex(value, flags) => Json.obj("$regex" -> value, "$options" -> flags)
      case BSONNull => JsNull
      case BSONUndefined => JsUndefined("")
      case BSONInteger(value) => JsNumber(value)
      case BSONLong(value) => JsNumber(value)
      case BSONBinary(value, subType) =>
        val arr = new Array[Byte](value.readableBytes())
        value.readBytes(arr)
        Json.obj(
          "$binary" -> Converters.hex2Str(arr), 
          "$type" -> Converters.hex2Str(Array(subType.value.toByte))
        )
      case BSONDBPointer(value, id) => Json.obj("$ref" -> value, "$id" -> Converters.hex2Str(id))
      // NOT STANDARD AT ALL WITH JSON and MONGO
      case BSONJavaScript(value) => Json.obj("$js" -> value)
      case BSONSymbol(value) => Json.obj("$sym" -> value)
      case BSONJavaScriptWS(value) => Json.obj("$jsws" -> value)
      case BSONMinKey => Json.obj("$minkey" -> 0)
      case BSONMaxKey => Json.obj("$maxkey" -> 0)
    })

  object JsArrayReader extends BSONReader[JsArray] {
    def read(buffer: ChannelBuffer): JsArray = {
      val it = DefaultBSONReader.read(buffer)

      it.foldLeft(Json.arr()) { (acc: JsArray, e: BSONElement) => acc :+ toTuple(e)._2 }
    }
  }

  object JsObjectReader extends BSONReader[JsObject] {
    def read(buffer: ChannelBuffer): JsObject = {
      val it = DefaultBSONReader.read(buffer)

      JsObject(it.foldLeft(List[(String, JsValue)]()) { (acc, e) => acc :+ toTuple(e) })
    }
  }

  implicit object JsValueReader extends BSONReader[JsValue] {
    def read(buffer: ChannelBuffer): JsValue = JsObjectReader.read(buffer)
  }

}