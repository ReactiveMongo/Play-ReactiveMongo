package play.modules.mongodb

import play.api.data.resource._
import play.api.libs.json._
import org.asyncmongo.bson._
import org.asyncmongo.handlers._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import play.api.libs.concurrent._
import org.asyncmongo.protocol.messages.GetLastError
import org.asyncmongo.api.Cursor
import play.api.libs.iteratee.Enumerator

import PlayBsonImplicits._

case class MongoTemplate[T](collection: String)(implicit bw: BSONWriter[T], br: BSONReader[T]) extends ResourceTemplate[T] {
  import play.api.Play.current

  val coll = MongoAsyncPlugin.collection(collection)

  def insert(t: T): Promise[ResourceResult[T]] = {
    coll.insert(t, GetLastError()).asPromise.map { lastError =>
      if(lastError.ok) ResourceSuccess(t)
      else ResourceOpError(Seq(ResourceErrorMsg("DB_ERROR", "resource.error.insert", lastError.stringify)))
    }
  }

  def findOne(json: JsValue): Promise[ResourceResult[T]] = {
    val future = coll.find[JsValue, JsValue, T](json, None, 0, 0)

    future.asPromise.map( cur => 
      if(cur.iterator.hasNext) ResourceSuccess(cur.iterator.next)
      else ResourceOpError(Seq(ResourceErrorMsg("DB_ERROR", "resource.error.notfound")))
    )
  }

  def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]] = {
    val future = coll.find[JsValue, JsValue, T](json, None, 0, 0)

    Promise.pure(ResourceSuccess(Cursor.enumerate(Some(future))))
  }
} 