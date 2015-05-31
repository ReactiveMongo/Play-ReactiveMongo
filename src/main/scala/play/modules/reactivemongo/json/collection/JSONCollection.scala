package play.modules.reactivemongo.json.collection

import play.api.libs.json._
import reactivemongo.api.collections.{ GenericCollection, GenericQueryBuilder }
import reactivemongo.api.{ CollectionMetaCommands, DB, FailoverStrategy }
import reactivemongo.core.commands.{ GetLastError, LastError }

import scala.concurrent.{ ExecutionContext, Future }

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
    import play.modules.reactivemongo.json.BSONFormats
    import reactivemongo.bson._
    doc \ "_id" match {
      case _: JsUndefined => insert(doc + ("_id" -> BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)), writeConcern)
      case JsDefined(id)  => update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    }
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
