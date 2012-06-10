package play.modules.mongodb

import play.api.data.resource._
import play.api.libs.json._
import org.asyncmongo.bson._
import org.asyncmongo.handlers._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import play.api.libs.concurrent._

import PlayBsonImplicits._

case class MongoTemplate(collection: String) extends ResourceTemplate[JsValue] {
  import play.api.Play.current

  val coll = MongoAsyncPlugin.collection(collection)

  def insert(json: JsValue): Promise[ResourceResult[JsValue]] = {
    coll.insert(json)
    Promise.pure(ResourceSuccess(json))
  }

  def findOne(json: JsValue): Promise[ResourceResult[JsValue]] = {
    val future = coll.find[JsValue, JsValue, JsValue](json, None, 0, 0)

    new AkkaFuture(future).asPromise.map( cur => 
      if(cur.iterator.hasNext) ResourceSuccess(cur.iterator.next)
      else ResourceOpError(Seq(ResourceErrorMsg("DB_ERROR", "resource.notfound")))
    )
  }
} 