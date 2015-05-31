package play.modules.reactivemongo.json.collection

import play.api.libs.json._
import reactivemongo.api.collections._
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer.ReadableBuffer

object JSONGenericHandlers extends JSONGenericHandlers

trait JSONGenericHandlers extends GenericHandlers[JsObject, Reads, Writes] {
  import play.modules.reactivemongo.json.BSONFormats._
  object StructureBufferReader extends BufferReader[JsObject] {
    def read(buffer: ReadableBuffer) = {
      Json.toJson(BSONDocument.read(buffer)).as[JsObject]
    }
  }
  object StructureBufferWriter extends BufferWriter[JsObject] {
    def write[B <: reactivemongo.bson.buffer.WritableBuffer](document: JsObject, buffer: B): B = {
      BSONDocument.write(Json.fromJson[BSONDocument](document).get, buffer)
      buffer
    }
  }
  case class BSONStructureReader[T](reader: Reads[T]) extends GenericReader[JsObject, T] {
    def read(doc: JsObject) = reader.reads(doc) match {
      case success: JsSuccess[T] => success.get
      case error: JsError        => throw new NoSuchElementException(error.toString)
    }
  }
  case class BSONStructureWriter[T](writer: Writes[T]) extends GenericWriter[T, JsObject] {
    def write(t: T) = writer.writes(t).as[JsObject]
  }
  def StructureReader[T](reader: Reads[T]) = BSONStructureReader(reader)
  def StructureWriter[T](writer: Writes[T]): GenericWriter[T, JsObject] = BSONStructureWriter(writer)
}