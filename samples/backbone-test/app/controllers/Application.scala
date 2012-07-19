package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.modules.mongodb._
import play.modules.mongodb.PlayBsonImplicits._
import org.asyncmongo.api._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.bson._
import org.asyncmongo.protocol._
import akka.dispatch.Future

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.data.validation.Constraints._

import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee._

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

  def watchZEvents = WebSocket.using[JsValue] { request => 
    val in = Iteratee.foreach[JsValue] { json =>
      println("received " + json)
      coll.insert(json)
    }
    ( 
      in, 
      out &> Enumeratee.filter{ js => 
        (js \ "title").validate[String].asOpt.exists(_.contains("chboing"))
      }
    )
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
