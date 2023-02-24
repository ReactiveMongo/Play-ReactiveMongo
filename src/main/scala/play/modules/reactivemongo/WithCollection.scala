package play.modules.reactivemongo

import scala.concurrent.{ ExecutionContext, Future }

import reactivemongo.api.{ Collection, CollectionProducer, DB }

/**
 * {{{
 * import scala.concurrent.Future
 *
 * import reactivemongo.api.DB
 *
 * import reactivemongo.api.bson.collection.BSONCollection
 * import play.modules.reactivemongo.WithCollection
 *
 * class MyComponent(
 *   val collectionName: String) extends WithCollection[BSONCollection] {
 *   def database: Future[DB] = ???
 * }
 * }}}
 */
trait WithCollection[C <: Collection] {

  /** Database asynchronous reference */
  def database: Future[DB]

  /** The name of the collection */
  def collectionName: String

  /** Resolve a reference to the collection specified by its name. */
  final def collection(implicit
      ec: ExecutionContext,
      cp: CollectionProducer[C]
    ): Future[C] = database.map(_(collectionName))
}

/**
 * {{{
 * import scala.concurrent.Future
 *
 * import reactivemongo.api.DB
 *
 * import reactivemongo.api.bson.collection.BSONCollection
 * import play.modules.reactivemongo.CollectionResolution
 *
 * class MyComponent extends CollectionResolution[BSONCollection]("collName") {
 *   def database: Future[DB] = ???
 * }
 * }}}
 */
abstract class CollectionResolution[C <: Collection](
    val collectionName: String)
    extends WithCollection[C]
