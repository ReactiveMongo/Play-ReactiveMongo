package controllers

import play.api._
import play.api.mvc._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import play.api.libs.json._
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import ReactiveMongoImplicits._
import play.api.mvc.Results.Status
import play.api.libs.iteratee.Enumerator

object ReactiveMongoImplicits {
  import play.api.libs.concurrent.Execution.Implicits._
//  implicit def FlattenedCursorToWritable(value: FlattenedCursor[JsValue]) =  value.toList.map { persons =>
//      Status(200)(persons.foldLeft(JsArray(List()))( (obj, person) => obj ++ Json.arr(person) ))
//  }
}
  
object Application extends Controller with MongoController {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("persons")


  val form = Form(
    tuple(
      "name" -> text,
      "age" -> number
    )
 )

  def index = Action { Ok(views.html.index("Hi there")) }
  
  def list = Action {
    Async {
      collection.find[JsValue]( QueryBuilder()).toList.map { persons =>
        Ok(persons.foldLeft(JsArray(List()))( (obj, person) => obj ++ Json.arr(person) ))
      }
    }
  }
  
  def create = Action { implicit request => {
      Async {
        val (name, age) = form.bindFromRequest.get
        val json = Json.obj(
          "name" -> name, 
          "age" -> age,
          "created" -> new java.util.Date().getTime()
        )

        collection.insert[JsValue]( json ).map( lastError =>
          Ok("Mongo LastErorr:%s".format(lastError))
        )
      }
    }
  }
 
  def createFromJson = Action(parse.json) {  request =>
    Async {
      collection.insert[JsValue]( request.body ).map( lastError =>
        Ok("Mongo LastErorr:%s".format(lastError))
      )
    }
  }
 
  def findByName(name: String) = Action {
    Async {
      val qb = QueryBuilder().query(Json.obj( "name" -> name )).sort( "created" -> SortOrder.Descending)

      collection.find[JsValue]( qb ).toList.map { persons =>
        Ok(persons.foldLeft(JsArray(List()))( (obj, person) => obj ++ Json.arr(person) ))
      }
    }
  } 

}