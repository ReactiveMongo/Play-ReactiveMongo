package controllers

import play.api._
import play.api.mvc._

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

// Play Json imports
import play.api.libs.json._

import play.api.Play.current

object Application extends Controller with MongoController {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("persons")
  
  def index = Action { Ok("works") }

  def create(name: String, age: Int) = Action {
    Async {
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