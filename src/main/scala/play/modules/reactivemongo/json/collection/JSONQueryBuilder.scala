package play.modules.reactivemongo.json.collection

import play.api.libs.json._
import reactivemongo.api.collections.{ BufferReader, GenericQueryBuilder }
import reactivemongo.api.{ Collection, FailoverStrategy, QueryOpts }
import reactivemongo.bson.buffer.WritableBuffer

case class JSONQueryBuilder(
    collection: Collection,
    failover: FailoverStrategy,
    queryOption: Option[JsObject] = None,
    sortOption: Option[JsObject] = None,
    projectionOption: Option[JsObject] = None,
    hintOption: Option[JsObject] = None,
    explainFlag: Boolean = false,
    snapshotFlag: Boolean = false,
    commentString: Option[String] = None,
    options: QueryOpts = QueryOpts()) extends GenericQueryBuilder[JsObject, Reads, Writes] with JSONGenericHandlers {
  import reactivemongo.utils.option
  type Self = JSONQueryBuilder

  private def empty = Json.obj()

  protected def writeStructureIntoBuffer[B <: WritableBuffer](document: JsObject, buffer: B): B = {
    JSONGenericHandlers.StructureBufferWriter.write(document, buffer)
  }

  object structureReader extends Reads[JsObject] {
    def reads(json: JsValue): JsResult[JsObject] = json.validate[JsObject]
  }

  protected def toStructure[T](writer: Writes[T], subject: T) = writer.writes(subject)

  def convert[T](reader: Reads[T]): BufferReader[T] = JSONDocumentReaderAsBufferReader(reader)

  def copy(queryOption: Option[JsObject], sortOption: Option[JsObject], projectionOption: Option[JsObject], hintOption: Option[JsObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy): JSONQueryBuilder =
    JSONQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options)

  def merge: JsObject = {
    if (sortOption.isEmpty && hintOption.isEmpty && !explainFlag && !snapshotFlag && commentString.isEmpty)
      queryOption.getOrElse(Json.obj())
    else {
      Json.obj("$query" -> (queryOption.getOrElse(empty): JsObject)) ++
        sortOption.map(o => Json.obj("$orderby" -> o)).getOrElse(empty) ++
        hintOption.map(o => Json.obj("$hint" -> o)).getOrElse(empty) ++
        commentString.map(o => Json.obj("$comment" -> o)).getOrElse(empty) ++
        option(explainFlag, JsBoolean(true)).map(o => Json.obj("$explain" -> o)).getOrElse(empty) ++
        option(snapshotFlag, JsBoolean(true)).map(o => Json.obj("$snapshot" -> o)).getOrElse(empty)
    }
  }
}
