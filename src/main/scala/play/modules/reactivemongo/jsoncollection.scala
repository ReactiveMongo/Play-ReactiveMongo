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

import scala.collection.immutable.ListSet

import play.api.libs.json.{
  Json,
  JsArray,
  JsBoolean,
  JsError,
  JsObject,
  JsPath,
  JsUndefined,
  JsSuccess,
  Writes
}

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
import reactivemongo.utils.option

import reactivemongo.play.json.{ BSONFormats, JSONSerializationPack }

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
@deprecated(
  "Use [[reactivemongo.play.json.collection]]", "0.12.0")
object `package` {
  implicit object JSONCollectionProducer extends GenericCollectionProducer[JSONSerializationPack.type, JSONCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONCollection(db, name, failoverStrategy)
  }
}

@deprecated(
  "Use [[reactivemongo.play.json.collection.JSONBatchCommands]]", "0.12.0")
object JSONBatchCommands
    extends BatchCommands[JSONSerializationPack.type] { commands =>

  import play.api.libs.json.{
    JsError,
    JsNull,
    JsNumber,
    JsValue,
    JsString,
    JsResult,
    JsSuccess,
    Reads,
    __
  }
  import reactivemongo.bson.{
    BSONArray,
    BSONDocument,
    BSONDocumentWriter,
    BSONObjectID,
    BSONValue,
    Producer
  }
  import reactivemongo.api.commands.{
    CountCommand => CC,
    DefaultWriteResult,
    DeleteCommand => DC,
    GetLastError => GLE,
    InsertCommand => IC,
    DistinctCommand => DistC,
    LastError,
    ResolvedCollectionCommand,
    UpdateCommand => UC,
    Upserted,
    UpdateWriteResult,
    WriteError,
    WriteConcernError
  }
  import reactivemongo.play.json.readOpt

  val pack = JSONSerializationPack

  object JSONCountCommand extends CC[JSONSerializationPack.type] {
    val pack = commands.pack
  }
  val CountCommand = JSONCountCommand

  object JSONDistinctCommand extends DistC[JSONSerializationPack.type] {
    val pack = commands.pack
  }
  val DistinctCommand = JSONDistinctCommand

  implicit object DistinctWriter
      extends pack.Writer[ResolvedCollectionCommand[DistinctCommand.Distinct]] {

    def writes(cmd: ResolvedCollectionCommand[DistinctCommand.Distinct]): pack.Document = Json.obj(
      "distinct" -> cmd.collection,
      "key" -> cmd.command.keyString,
      "query" -> cmd.command.query)
  }

  implicit object DistinctResultReader
      extends pack.Reader[DistinctCommand.DistinctResult] {

    private val path = JsPath \ "values"

    def reads(js: JsValue): JsResult[DistinctCommand.DistinctResult] =
      (js \ "values").toEither match {
        case Right(JsArray(values)) =>
          JsSuccess(DistinctCommand.DistinctResult(ListSet.empty ++ values))

        case Right(v)    => JsError(path, s"invalid JSON: $v")
        case Left(error) => JsError(Seq(path -> Seq(error)))
      }
  }

  implicit object HintWriter extends Writes[CountCommand.Hint] {
    import CountCommand.{ HintString, HintDocument }

    def writes(hint: CountCommand.Hint): JsValue = hint match {
      case HintString(s)     => JsString(s)
      case HintDocument(obj) => obj
    }
  }

  implicit object CountWriter
      extends pack.Writer[ResolvedCollectionCommand[CountCommand.Count]] {

    def writes(count: ResolvedCollectionCommand[CountCommand.Count]): pack.Document = {
      val fields = Seq[Option[(String, Json.JsValueWrapper)]](
        Some("count" -> count.collection),
        count.command.query.map("query" -> _),
        option(count.command.limit != 0, count.command.limit).map("limit" -> _),
        option(count.command.skip != 0, count.command.skip).map("skip" -> _),
        count.command.hint.map("hint" -> _)).flatten

      Json.obj(fields: _*)
    }
  }

  implicit object CountResultReader
      extends pack.Reader[CountCommand.CountResult] {
    def reads(js: JsValue): JsResult[CountCommand.CountResult] =
      (js \ "n").validate[Int].map(CountCommand.CountResult(_))
  }

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
      id <- (js \ "_id").validate[JsValue].flatMap(BSONFormats.toBSON)
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
      ok <- readOpt[Int](js \ "ok")
      n <- readOpt[Int](js \ "n")
      mo <- readOpt[Int](js \ "nModified")
      up <- readOpt[Seq[Upserted]](js \ "upserted")
      we <- readOpt[Seq[WriteError]](js \ "writeErrors")
      ce <- readOpt[WriteConcernError](js \ "writeConcernError")
      co <- readOpt[Int](js \ "code") //FIXME There is no corresponding official docs.
      em <- readOpt[String](js \ "errmsg") //FIXME There is no corresponding official docs.
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
      ok <- readOpt[Int](js \ "ok")
      n <- readOpt[Int](js \ "n")
      we <- readOpt[Seq[WriteError]](js \ "writeErrors")
      ce <- readOpt[WriteConcernError](js \ "writeConcernError")
      co <- readOpt[Int](js \ "code") //FIXME There is no corresponding official docs.      
      em <- readOpt[String](js \ "errmsg") //FIXME There is no corresponding official docs.
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
      ok <- readOpt[Int](js \ "ok")
      er <- readOpt[String](js \ "err")
      co <- readOpt[Int](js \ "code")
      lo <- readOpt[Long](js \ "lastOp")
      n <- readOpt[Int](js \ "n")
      ss <- readOpt[String](js \ "singleShard")
      ux <- readOpt[Boolean](js \ "updatedExisting")
      ue <- readOpt[JsValue](js \ "upserted").flatMap(
        _.fold[JsResult[Option[BSONValue]]](
          JsSuccess(None))(BSONFormats.toBSON(_).map(Some(_))))
      wn <- (js \ "wnote").get match {
        case JsString("majority") => JsSuccess(Some(GLE.Majority))
        case JsString(tagSet)     => JsSuccess(Some(GLE.TagSet(tagSet)))
        case JsNumber(acks) => JsSuccess(
          Some(GLE.WaitForAknowledgments(acks.toInt)))
        case _ => JsSuccess(Option.empty[GLE.W])
      }
      wt <- readOpt[Boolean](js \ "wtimeout")
      we <- readOpt[Int](js \ "waited")
      wm <- readOpt[Int](js \ "wtime")
    } yield LastError(ok.exists(_ != 0), er, co, lo, n.getOrElse(0),
      ss, ux.getOrElse(false), ue, wn, wt.getOrElse(false), we, wm)
  }

  import play.modules.reactivemongo.json.commands.{
    JSONFindAndModifyCommand,
    JSONFindAndModifyImplicits
  }
  val FindAndModifyCommand = JSONFindAndModifyCommand
  implicit def FindAndModifyWriter = JSONFindAndModifyImplicits.FindAndModifyWriter
  implicit def FindAndModifyReader = JSONFindAndModifyImplicits.FindAndModifyResultReader

  import play.modules.reactivemongo.json.commands.{
    JSONAggregationFramework,
    JSONAggregationImplicits
  }
  val AggregationFramework = JSONAggregationFramework
  implicit def AggregateWriter = JSONAggregationImplicits.AggregateWriter
  implicit def AggregateReader =
    JSONAggregationImplicits.AggregationResultReader

}

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
@deprecated(
  "Use [[reactivemongo.play.json.collection.JSONCollection]]", "0.12.0")
case class JSONCollection(
  db: DB, name: String, failoverStrategy: FailoverStrategy)
    extends GenericCollection[JSONSerializationPack.type] with CollectionMetaCommands {

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
  @deprecated("0.11.1", "Use [[update]] with `upsert` set to true")
  def save(doc: JsObject)(implicit ec: ExecutionContext): Future[WriteResult] =
    save(doc, WriteConcern.Default)

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern The write concern
   */
  @deprecated("0.11.1", "Use [[update]] with `upsert` set to true")
  def save(doc: pack.Document, writeConcern: WriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
    import reactivemongo.bson.BSONObjectID
    (doc \ "_id").toOption match {
      case None => insert(doc + ("_id" ->
        BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)),
        writeConcern)

      case Some(id) =>
        update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    }
  }

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern The write concern
   */
  @deprecated("0.11.1", "Use [[update]] with `upsert` set to true")
  def save[T](doc: T, writeConcern: WriteConcern = WriteConcern.Default)(implicit ec: ExecutionContext, writer: Writes[T]): Future[WriteResult] =
    writer.writes(doc) match {
      case d @ JsObject(_) => save(d, writeConcern)
      case _ =>
        Future.failed[WriteResult](new Exception("cannot write object"))
    }
}

@deprecated(
  "Use [[reactivemongo.play.json.collection.JSONQueryBuilder]]", "0.12.0")
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
  options: QueryOpts = QueryOpts(),
  maxTimeMsOption: Option[Long] = None)
    extends GenericQueryBuilder[JSONSerializationPack.type] {

  import play.api.libs.json.Json.JsValueWrapper

  type Self = JSONQueryBuilder

  val pack = JSONSerializationPack
  private def empty = Json.obj()

  def copy(queryOption: Option[JsObject], sortOption: Option[JsObject], projectionOption: Option[JsObject], hintOption: Option[JsObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy, maxTimeMsOption: Option[Long]): JSONQueryBuilder =
    JSONQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options, maxTimeMsOption)

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
      maxTimeMsOption.map { "$maxTimeMS" -> _ },
      commentString.map { "$comment" -> _ },
      option(explainFlag, "$explain" -> true),
      option(snapshotFlag, "$snapshot" -> true),
      readPreferenceDocument.map { "$readPreference" -> _ }).flatten

    val query = queryOption.getOrElse(empty)

    if (optionalFields.isEmpty) query else {
      val fs = ("$query" -> implicitly[JsValueWrapper](query)) :: optionalFields
      Json.obj(fs: _*)
    }
  }
}

// JSON extension for cursors

import reactivemongo.api.{
  Cursor,
  CursorProducer,
  FlattenedCursor,
  WrappedCursor
}

@deprecated(
  "Use [[reactivemongo.play.json.collection.JsCursor]]", "0.12.0")
sealed trait JsCursor[T] extends Cursor[T] {
  /**
   * Returns the result of cursor as a JSON array.
   *
   * @param maxDocs Maximum number of documents to be retrieved
   */
  def jsArray(maxDocs: Int = Int.MaxValue)(implicit ec: ExecutionContext): Future[JsArray]

}

@deprecated(
  "Use [[reactivemongo.play.json.collection.JsCursorImpl]]", "0.12.0")
class JsCursorImpl[T: Writes](val wrappee: Cursor[T])
    extends JsCursor[T] with WrappedCursor[T] {
  import Cursor.{ Cont, Fail }

  private val writes = implicitly[Writes[T]]

  def jsArray(maxDocs: Int = Int.MaxValue)(implicit ec: ExecutionContext): Future[JsArray] = wrappee.foldWhile(Json.arr(), maxDocs)(
    (arr, res) => Cont(arr :+ writes.writes(res)),
    (_, error) => Fail(error))

}

@deprecated(
  "Use [[reactivemongo.play.json.collection.JsFlattenedCursor]]", "0.12.0")
class JsFlattenedCursor[T](val future: Future[JsCursor[T]])
    extends FlattenedCursor[T](future) with JsCursor[T] {

  def jsArray(maxDocs: Int = Int.MaxValue)(implicit ec: ExecutionContext): Future[JsArray] = future.flatMap(_.jsArray(maxDocs))

}

/** Implicits of the JSON extensions for cursors. */
@deprecated(
  "Use [[reactivemongo.play.json.collection.JsCursor]]", "0.12.0")
object JsCursor {
  import reactivemongo.api.{ CursorFlattener, CursorProducer }

  /** Provides JSON instances for CursorProducer typeclass. */
  implicit def cursorProducer[T: Writes] = new CursorProducer[T] {
    type ProducedCursor = JsCursor[T]

    // Returns a cursor with JSON operations.
    def produce(base: Cursor[T]): JsCursor[T] = new JsCursorImpl[T](base)
  }

  /** Provides flattener for JSON cursor. */
  implicit object cursorFlattener extends CursorFlattener[JsCursor] {
    def flatten[T](future: Future[JsCursor[T]]): JsCursor[T] =
      new JsFlattenedCursor(future)
  }
}
