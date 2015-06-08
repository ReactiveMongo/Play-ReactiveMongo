package play.modules.reactivemongo.json.collection

import play.api.libs.json.Reads
import reactivemongo.api.collections.BufferReader
import reactivemongo.bson.buffer.ReadableBuffer

case class JSONDocumentReaderAsBufferReader[T](reader: Reads[T]) extends BufferReader[T] {
  def read(buffer: ReadableBuffer) = reader.reads(JSONGenericHandlers.StructureBufferReader.read(buffer)).get
}
