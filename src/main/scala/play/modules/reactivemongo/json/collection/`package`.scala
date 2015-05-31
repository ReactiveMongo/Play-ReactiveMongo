package play.modules.reactivemongo.json.collection

import play.api.libs.json.{ JsObject, Reads, Writes }
import reactivemongo.api.collections.GenericCollectionProducer
import reactivemongo.api.{ DB, FailoverStrategy }

/**
 * A Collection that interacts with the Play JSON library, using `Reads` and `Writes`.
 */
object `package` {
  implicit object JSONCollectionProducer extends GenericCollectionProducer[JsObject, Reads, Writes, JSONCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONCollection(db, name, failoverStrategy)
  }
}
