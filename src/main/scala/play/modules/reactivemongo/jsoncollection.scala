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
package play.modules.reactivemongo.json.collection

import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.collections._
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer._
import reactivemongo.core.commands.{ GetLastError, LastError }
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
object `package` {
  implicit object JSONCollectionProducer extends GenericCollectionProducer[JsObject, Reads, Writes, JSONCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONCollection(db, name, failoverStrategy)
  }
}

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

object JSONGenericHandlers extends JSONGenericHandlers

case class JSONDocumentReaderAsBufferReader[T](reader: Reads[T]) extends BufferReader[T] {
  def read(buffer: ReadableBuffer) = reader.reads(JSONGenericHandlers.StructureBufferReader.read(buffer)).get
}

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
case class JSONCollection(
    db: DB,
    name: String,
    failoverStrategy: FailoverStrategy) extends GenericCollection[JsObject, Reads, Writes] with JSONGenericHandlers with CollectionMetaCommands {
  def genericQueryBuilder: GenericQueryBuilder[JsObject, Reads, Writes] =
    JSONQueryBuilder(this, failoverStrategy)

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   */
  def save(doc: JsObject)(implicit ec: ExecutionContext): Future[LastError] =
    save(doc, GetLastError())

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern the [[reactivemongo.core.commands.GetLastError]] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   */
  def save(doc: JsObject, writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    import reactivemongo.bson._
    import play.modules.reactivemongo.json.BSONFormats
    (doc \ "_id" match {
      case _: JsUndefined => insert(doc + ("_id" -> BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)), writeConcern)
      case id             => update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    })
  }

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern the [[reactivemongo.core.commands.GetLastError]] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   */
  def save[T](doc: T, writeConcern: GetLastError = GetLastError())(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] =
    save(writer.writes(doc).as[JsObject], writeConcern)
}

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
    if (!sortOption.isDefined && !hintOption.isDefined && !explainFlag && !snapshotFlag && !commentString.isDefined)
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
