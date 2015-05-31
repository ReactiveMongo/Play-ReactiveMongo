package play.modules.reactivemongo.json

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPluginException
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters

/**
 * JSON Formats for BSONValues.
 */
object BSONFormats {
  trait PartialFormat[T <: BSONValue] extends Format[T] {
    def partialReads: PartialFunction[JsValue, JsResult[T]]
    def partialWrites: PartialFunction[BSONValue, JsValue]

    def writes(t: T): JsValue = partialWrites(t)
    def reads(json: JsValue) = partialReads.lift(json).getOrElse(JsError(s"Unhandled json value! [$json]"))
  }

  implicit object BSONDoubleFormat extends PartialFormat[BSONDouble] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONDouble]] = {
      case JsNumber(f)        => JsSuccess(BSONDouble(f.toDouble))
      case DoubleValue(value) => JsSuccess(BSONDouble(value))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case double: BSONDouble => JsNumber(double.value)
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
          JsSuccess(BSONDocument(obj.fields.map { tuple =>
            tuple._1 -> (toBSON(tuple._2) match {
              case JsSuccess(bson, _) => bson
              case JsError(err)       => throw new ReactiveMongoPluginException(err.toString())
            })
          }))
        } catch {
          case e: Throwable => JsError(e.getMessage)
        }
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case doc: BSONDocument => JsObject(doc.elements.map(e => e._1 -> toJSON(e._2)).toSeq)
    }
  }
  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)
  class BSONArrayFormat(toBSON: JsValue => JsResult[BSONValue], toJSON: BSONValue => JsValue) extends PartialFormat[BSONArray] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONArray]] = {
      case arr: JsArray =>
        try {
          JsSuccess(BSONArray(arr.value.map { value =>
            toBSON(value) match {
              case JsSuccess(bson, _) => bson
              case JsError(err)       => throw new ReactiveMongoPluginException(err.toString())
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
  }
  implicit object BSONTimestampFormat extends PartialFormat[BSONTimestamp] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONTimestamp]] = {
      case TimeValue(value) => JsSuccess(BSONTimestamp(value))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case ts: BSONTimestamp => Json.obj("$time" -> ts.value.toInt, "i" -> (ts.value >>> 4))
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
          case (Some(rxValue), Some(optsValue)) => JsSuccess(BSONRegex(rxValue, optsValue))
          case (None, Some(_))                  => JsError(__ \ "$regex", "string expected")
          case (Some(_), None)                  => JsError(__ \ "$options", "string expected")
          case _                                => JsError(__ \ "$regex", "string expected") ++ JsError(__ \ "$options", "string expected")
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
  }
  implicit object BSONLongFormat extends PartialFormat[BSONLong] {
    val partialReads: PartialFunction[JsValue, JsResult[BSONLong]] = {
      case JsNumber(long)   => JsSuccess(BSONLong(long.toLong))
      case LongValue(value) => JsSuccess(BSONLong(value))
    }
    val partialWrites: PartialFunction[BSONValue, JsValue] = {
      case long: BSONLong => JsNumber(long.value)
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
    orElse(BSONStringFormat.partialWrites).
    orElse(BSONSymbolFormat.partialWrites).
    orElse(BSONArrayFormat.partialWrites).
    orElse(BSONDocumentFormat.partialWrites).
    lift(bson).getOrElse(throw new ReactiveMongoPluginException(s"Unhandled json value: $bson"))

  private object DoubleValue {
    def unapply(jsObject: JsObject): Option[Double] = getFieldValue(jsObject, "$double", getDouble)
    def getDouble(jsValue: JsValue): Option[Double] = jsValue match {
      case JsNumber(v) => Some(v.toDouble)
      case _           => None
    }
  }

  private object OidValue {
    def unapply(jsObject: JsObject): Option[String] = getFieldValue(jsObject, "$oid", SymbolValue.getString)
  }

  private object DateValue {
    def unapply(jsObject: JsObject): Option[Long] = getFieldValue(jsObject, "$date", LongValue.getLong)
  }

  private object TimeValue {
    def unapply(jsObject: JsObject): Option[Long] = getFieldValue(jsObject, "$time", LongValue.getLong)
  }

  private object IntValue {
    def unapply(jsObject: JsObject): Option[Int] = getFieldValue(jsObject, "$int", getInt)
    def getInt(jsValue: JsValue): Option[Int] = jsValue match {
      case JsNumber(v) => Some(v.toInt)
      case _           => None
    }
  }

  private object LongValue {
    def unapply(jsObject: JsObject): Option[Long] = getFieldValue(jsObject, "$long", getLong)
    def getLong(jsValue: JsValue): Option[Long] = jsValue match {
      case JsNumber(v) => Some(v.toLong)
      case _           => None
    }
  }

  private object SymbolValue {
    def unapply(jsObject: JsObject): Option[String] = getFieldValue(jsObject, "$symbol", getString)
    def getString(jsValue: JsValue): Option[String] = jsValue match {
      case JsString(v) => Some(v)
      case _           => None
    }
  }

  private def getFieldValue[T](jsObject: JsObject, fieldName: String, f: JsValue => Option[T]): Option[T] = {
    if (jsObject.fields.size != 1 || jsObject.fields.head._1 != fieldName)
      None
    else
      f(jsObject.fields.head._2)
  }
}
