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

import scala.concurrent.{ ExecutionContext, Future }

import play.api.libs.json.{ Json, JsBoolean, JsObject, JsUndefined, Writes }

import reactivemongo.api.{
  Collection,
  CollectionMetaCommands,
  DB,
  FailoverStrategy,
  QueryOpts,
  ReadPreference
}
import reactivemongo.api.collections.{
  BatchCommands,
  GenericCollection,
  GenericCollectionProducer,
  GenericQueryBuilder
}
import reactivemongo.api.commands.{ WriteConcern, WriteResult }

import play.modules.reactivemongo.json.{ BSONFormats, JSONSerializationPack }

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
object `package` {
  implicit object JSONCollectionProducer extends GenericCollectionProducer[JSONSerializationPack.type, JSONCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONCollection(db, name, failoverStrategy)
  }
}

object JSONBatchCommands
    extends BatchCommands[JSONSerializationPack.type] { commands =>

  import play.api.libs.json.{
    JsError,
    JsNull,
    JsNumber,
    JsValue,
    JsString,
    JsResult,
    JsSuccess
  }
  import reactivemongo.bson.{
    BSONArray,
    BSONDocument,
    BSONDocumentWriter,
    BSONObjectID,
    Producer
  }, Producer._
  import reactivemongo.api.commands.{
    DefaultWriteResult,
    DeleteCommand => DC,
    GetLastError => GLE,
    InsertCommand => IC,
    LastError,
    ResolvedCollectionCommand,
    UpdateCommand => UC,
    Upserted,
    UpdateWriteResult,
    WriteError,
    WriteConcernError
  }

  val pack = JSONSerializationPack

  object JSONInsertCommand extends IC[JSONSerializationPack.type] {
    val pack = commands.pack
  }
  val InsertCommand = JSONInsertCommand
  type ResolvedInsert = ResolvedCollectionCommand[InsertCommand.Insert]

  implicit object WriteConcernWriter extends pack.Writer[WriteConcern] {
    def writes(wc: WriteConcern): pack.Document = {
      val obj = Json.obj(
        "w" -> ((wc.w match {
          case GLE.Majority                 => JsString("majority")
          case GLE.TagSet(tagSet)           => JsString(tagSet)
          case GLE.WaitForAknowledgments(n) => JsNumber(n)
        }): JsValue),
        "wtimeout" -> wc.wtimeout)

      if (!wc.j) obj else obj + ("j" -> JsBoolean(true))
    }
  }

  implicit object InsertWriter extends pack.Writer[ResolvedInsert] {
    def writes(cmd: ResolvedInsert): pack.Document = Json.obj(
      "insert" -> cmd.collection,
      "documents" -> cmd.command.documents,
      "ordered" -> cmd.command.ordered,
      "writeConcern" -> cmd.command.writeConcern)
  }

  object JSONUpdateCommand extends UC[JSONSerializationPack.type] {
    val pack = commands.pack
  }
  val UpdateCommand = JSONUpdateCommand
  type ResolvedUpdate = ResolvedCollectionCommand[UpdateCommand.Update]

  implicit object UpdateElementWriter
      extends pack.Writer[UpdateCommand.UpdateElement] {

    def writes(element: UpdateCommand.UpdateElement): pack.Document = Json.obj(
      "q" -> element.q,
      "u" -> element.u,
      "upsert" -> element.upsert,
      "multi" -> element.multi)
  }

  implicit object UpdateWriter extends pack.Writer[ResolvedUpdate] {
    def writes(cmd: ResolvedUpdate): pack.Document = Json.obj(
      "update" -> cmd.collection,
      "updates" -> Json.toJson(cmd.command.documents),
      "ordered" -> cmd.command.ordered,
      "writeConcern" -> cmd.command.writeConcern)
  }

  implicit object UpsertedReader extends pack.Reader[Upserted] {
    def reads(js: JsValue): JsResult[Upserted] = for {
      ix <- (js \ "index").validate[Int]
      id <- JsSuccess(BSONFormats.BSONObjectIDFormat.
        partialReads.lift(js \ "_id"))
    } yield Upserted(index = ix, _id = id)
  }

  implicit object WriteErrorReader extends pack.Reader[WriteError] {
    def reads(js: JsValue): JsResult[WriteError] = for {
      id <- (js \ "index").validate[Int]
      co <- (js \ "code").validate[Int]
      em <- (js \ "errmsg").validate[String]
    } yield WriteError(index = id, code = co, errmsg = em)
  }

  implicit object WriteConcernErrorReader
      extends pack.Reader[WriteConcernError] {
    def reads(js: JsValue): JsResult[WriteConcernError] = for {
      co <- (js \ "code").validate[Int]
      em <- (js \ "errmsg").validate[String]
    } yield WriteConcernError(code = co, errmsg = em)
  }

  implicit object UpdateReader extends pack.Reader[UpdateCommand.UpdateResult] {
    def reads(js: JsValue): JsResult[UpdateCommand.UpdateResult] = for {
      ok <- (js \ "ok").validate[Option[Int]]
      n <- (js \ "n").validate[Option[Int]]
      mo <- (js \ "nModified").validate[Option[Int]]
      up <- (js \ "upserted").validate[Option[Seq[Upserted]]]
      we <- (js \ "writeErrors").validate[Option[Seq[WriteError]]]
      ce <- (js \ "writeConcernError").validate[Option[WriteConcernError]]
      co <- (js \ "code").validate[Option[Int]] //FIXME There is no corresponding official docs.
      em <- (js \ "errmsg").validate[Option[String]] //FIXME There is no corresponding official docs.
    } yield UpdateWriteResult(
      ok = ok.exists(_ != 0),
      n = n.getOrElse(0),
      nModified = mo.getOrElse(0),
      upserted = up.getOrElse(Seq.empty[Upserted]),
      writeErrors = we.getOrElse(Seq.empty[WriteError]),
      writeConcernError = ce,
      code = co,
      errmsg = em)
  }

  object JSONDeleteCommand extends DC[JSONSerializationPack.type] {
    val pack = commands.pack
  }
  val DeleteCommand = JSONDeleteCommand
  type ResolvedDelete = ResolvedCollectionCommand[DeleteCommand.Delete]

  implicit object DeleteElementWriter
      extends pack.Writer[DeleteCommand.DeleteElement] {
    def writes(e: DeleteCommand.DeleteElement): pack.Document = Json.obj(
      "q" -> e.q, "limit" -> e.limit)
  }

  implicit object DeleteWriter extends pack.Writer[ResolvedDelete] {
    def writes(cmd: ResolvedDelete): pack.Document = Json.obj(
      "delete" -> cmd.collection,
      "deletes" -> Json.toJson(cmd.command.deletes),
      "ordered" -> cmd.command.ordered,
      "writeConcern" -> cmd.command.writeConcern)
  }

  implicit object DefaultWriteResultReader
      extends pack.Reader[DefaultWriteResult] {
    def reads(js: JsValue): JsResult[DefaultWriteResult] = for {
      ok <- (js \ "ok").validate[Option[Int]]
      n <- (js \ "n").validate[Option[Int]]
      we <- (js \ "writeErrors").validate[Option[Seq[WriteError]]]
      ce <- (js \ "writeConcernError").validate[Option[WriteConcernError]]
      co <- (js \ "code").validate[Option[Int]] //FIXME There is no corresponding official docs.      
      em <- (js \ "errmsg").validate[Option[String]] //FIXME There is no corresponding official docs.
    } yield DefaultWriteResult(
      ok = ok.exists(_ != 0),
      n = n.getOrElse(0),
      writeErrors = we.getOrElse(Seq.empty[WriteError]),
      writeConcernError = ce,
      code = co,
      errmsg = em)
  }

  implicit object LastErrorReader extends pack.Reader[LastError] {
    def reads(js: JsValue): JsResult[LastError] = for {
      ok <- (js \ "ok").validate[Option[Int]]
      er <- (js \ "err").validate[Option[String]]
      co <- (js \ "code").validate[Option[Int]]
      lo <- (js \ "lastOp").validate[Option[Long]]
      n <- (js \ "n").validate[Option[Int]]
      ss <- (js \ "singleShard").validate[Option[String]]
      ux <- (js \ "updatedExisting").validate[Option[Boolean]]
      ue <- BSONFormats.BSONObjectIDFormat.partialReads.lift(js \ "upserted").
        fold[JsResult[Option[BSONObjectID]]](
          JsSuccess(Option.empty[BSONObjectID]))(_.map(id => Some(id)))
      wn <- (js \ "wnote") match {
        case JsString("majority") => JsSuccess(Some(GLE.Majority))
        case JsString(tagSet)     => JsSuccess(Some(GLE.TagSet(tagSet)))
        case JsNumber(acks) => JsSuccess(
          Some(GLE.WaitForAknowledgments(acks.toInt)))
        case _ => JsSuccess(Option.empty[GLE.W])
      }
      wt <- (js \ "wtimeout").validate[Option[Boolean]]
      we <- (js \ "waited").validate[Option[Int]]
      wm <- (js \ "wtime").validate[Option[Int]]
    } yield LastError(ok.exists(_ != 0), er, co, lo, n.getOrElse(0),
      ss, ux.getOrElse(false), ue, wn, wt.getOrElse(false), we, wm)
  }
}

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
case class JSONCollection(
  db: DB, name: String, failoverStrategy: FailoverStrategy)
    extends GenericCollection[JSONSerializationPack.type]
    with CollectionMetaCommands {

  import reactivemongo.core.commands.GetLastError

  val pack = JSONSerializationPack
  val BatchCommands = JSONBatchCommands

  def genericQueryBuilder: GenericQueryBuilder[JSONSerializationPack.type] =
    JSONQueryBuilder(this, failoverStrategy)

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   */
  def save(doc: JsObject)(implicit ec: ExecutionContext): Future[WriteResult] =
    save(doc, WriteConcern.Default)

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern The write concern
   */
  def save(doc: pack.Document, writeConcern: WriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
    import reactivemongo.bson.BSONObjectID
    (doc \ "_id" match {
      case _: JsUndefined => insert(doc + ("_id" ->
        BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)),
        writeConcern)
      case id => update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    })
  }

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern The write concern
   */
  def save[T](doc: T, writeConcern: WriteConcern = WriteConcern.Default)(implicit ec: ExecutionContext, writer: Writes[T]): Future[WriteResult] =
    writer.writes(doc) match {
      case d @ JsObject(_) => save(d, writeConcern)
      case _ =>
        Future.failed[WriteResult](new Exception("cannot write object"))
    }
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
    options: QueryOpts = QueryOpts()) extends GenericQueryBuilder[JSONSerializationPack.type] {

  import play.api.libs.json.Json.JsValueWrapper
  import reactivemongo.utils.option

  type Self = JSONQueryBuilder

  val pack = JSONSerializationPack
  private def empty = Json.obj()

  def copy(queryOption: Option[JsObject], sortOption: Option[JsObject], projectionOption: Option[JsObject], hintOption: Option[JsObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy): JSONQueryBuilder =
    JSONQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options)

  def merge(readPreference: ReadPreference): JsObject = {
    // Primary and SecondaryPreferred are encoded as the slaveOk flag;
    // the others are encoded as $readPreference field.
    val readPreferenceDocument = readPreference match {
      case ReadPreference.Primary                    => None
      case ReadPreference.PrimaryPreferred(filter)   => Some(Json.obj("mode" -> "primaryPreferred"))
      case ReadPreference.Secondary(filter)          => Some(Json.obj("mode" -> "secondary"))
      case ReadPreference.SecondaryPreferred(filter) => None
      case ReadPreference.Nearest(filter)            => Some(Json.obj("mode" -> "nearest"))
    }

    val optionalFields = List[Option[(String, JsValueWrapper)]](
      sortOption.map { "$orderby" -> _ },
      hintOption.map { "$hint" -> _ },
      commentString.map { "$comment" -> _ },
      option(explainFlag, "$explain" -> true),
      option(snapshotFlag, "$snapshot" -> true),
      readPreferenceDocument.map { "$readPreference" -> _ }).flatten

    val query = queryOption.getOrElse(Json.obj())

    if (optionalFields.isEmpty) query
    else {
      val fs = ("$query" -> implicitly[JsValueWrapper](query)) :: optionalFields
      Json.obj(fs: _*)
    }
  }
}
