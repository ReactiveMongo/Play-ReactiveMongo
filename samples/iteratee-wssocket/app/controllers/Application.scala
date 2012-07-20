package controllers

import play.api._
import play.api.mvc._

import play.modules.mongodb._
import play.modules.mongodb.PlayBsonImplicits._

import org.asyncmongo.api._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.bson._
import org.asyncmongo.protocol._

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.data.validation.Constraints._
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Constraints._
import play.api.cache.Cache

import akka.dispatch.Future

object Application extends Controller {
  val coll = MongoAsyncPlugin.collection("zevent")
  val d = new java.util.Date()
  
  def index = Action { implicit request =>
    Ok(views.html.index("zevents"))
  }
  
  def connect = Action {
    Ok("toto")
  }

  val out = Concurrent.broadcast(enumerate(
    coll.find[JsValue, JsValue, JsValue](Json.obj(), None, 0, 0, QueryFlags.TailableCursor | QueryFlags.AwaitData)
  ), () )._1 &> Concurrent.buffer(30)


  implicit val eventScan = JsTupler(
    JsPath \ "type" -> in(required[String] and valueEquals("event")),
    JsPath \ "data" -> in(
      JsTupler(JsPath \ "title" -> in(required[String])) 
      andThen of[JsObject]
    )
  )

  def watchZEvents = WebSocket.using[JsValue] { implicit request => 
    val uuid = java.util.UUID.randomUUID().toString()

    val in = Iteratee.foreach[JsValue] { json =>
      println("received " + json)
      json.validate(eventScan).fold(
        valid = { s: (String, JsObject) => coll.insert(s._2) },
        invalid = e => println("error:%s".format(e))
      )
    }

    ( 
      in, 
      out &> Enumeratee.filter{ js => 
        println("filtering %s".format(js))
        (js \ "title").asOpt[String].exists{ t => 
          Cache.getAs[List[String]]("f$"+uuid).exists{ filters => 
            filters.exists( t.contains(_) )
          }
        }
      } &> Enumeratee.map{ js => js ++ Json.obj("uuid" -> uuid) }
    )
  }

  def addFilters(uuid: String, filters: List[String]) = Action {
    val cid = "f$"+uuid
    Cache.getAs[List[String]](cid).map{ f => Cache.set(cid, f ++ filters) }

    Ok("Filters Added")
  }

  def enumerate[T](futureCursor: Future[Cursor[T]]):Enumerator[T] = {

    Enumerator.flatten(futureCursor.map { cursor =>

      Enumerator.unfoldM(cursor) { cursor =>
        if(cursor.iterator.hasNext)
          Promise.pure(Some((cursor,Some(cursor.iterator.next))))
        else if (cursor.hasNext)
          cursor.next.get.asPromise.map(c => Some((c,None)))
        else 
          Promise.pure(None)        
      }
    }.asPromise) &> Enumeratee.collect{ case Some(e) => e}

  }
}
