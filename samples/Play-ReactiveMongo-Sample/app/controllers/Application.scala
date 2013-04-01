package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

/*
 * Example using ReactiveMongo + Play JSON library.
 *
 * There are two approaches demonstrated in this controller:
 * - using JsObjects directly
 * - using case classes that can be turned into Json using Reads and Writes.
 *
 * Instead of using the default Collection implementation (which interacts with
 * BSON structures + BSONReader/BSONWriter), we use a specialized
 * implementation that works with JsObject + Reads/Writes.
 *
 * Of course, you can still use the default Collection implementation
 * (BSONCollection.) See ReactiveMongo examples to learn how to use it.
 */
object Application extends Controller with MongoController {
  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("persons")

  def index = Action { Ok("works") }

  def create(name: String, age: Int) = Action {
    Async {
      val json = Json.obj(
        "name" -> name,
        "age" -> age,
        "created" -> new java.util.Date().getTime())

      collection.insert(json).map(lastError =>
        Ok("Mongo LastError: %s".format(lastError)))
    }
  }

  def createFromJson = Action(parse.json) { request =>
    case class Bod(s:String)
    Async {
      /*
       * request.body is a JsValue.
       * There is an implicit Writes that turns this JsValue as a JsObject,
       * so you can call insert() with this JsValue.
       * (insert() takes a JsObject as parameter, or anything that can be
       * turned into a JsObject using a Writes.)
       */
      collection.insert(request.body).map(lastError =>
        Ok("Mongo LastErorr:%s".format(lastError)))
    }
  }

  def findByName(name: String) = Action {
    Async {
      // let's do our query
      val cursor: Cursor[JsObject] = collection.
        // find all people with name `name`
        find(Json.obj("name" -> name)).
        // sort them by creation date
        sort(Json.obj("created" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[JsObject]

      // gather all the JsObjects in a list
      val futurePersonsList: Future[List[JsObject]] = cursor.toList

      // transform the list into a JsArray
      val futurePersonsJsonArray: Future[JsArray] = futurePersonsList.map { persons =>
        Json.arr(persons)
      }

      // everything's ok! Let's reply with the array
      futurePersonsJsonArray.map { persons =>
        Ok(persons)
      }
    }
  }

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //
  import play.api.data.Form
  import models._
  import models.JsonFormats._

  def createCC = Action {
    val user = User(29, "John", "Smith", List(
      Feed("Slashdot news", "http://slashdot.org/slashdot.rdf")))
    // insert the user
    val futureResult = collection.insert(user)
    Async {
      // when the insert is performed, send a OK 200 result
      futureResult.map(_ => Ok)
    }
  }

  def findByNameCC(name: String) = Action {
    // let's do our query
    Async {
      val cursor: Cursor[User] = collection.
        // find all people with name `name`
        find(Json.obj("name" -> name)).
        // sort them by creation date
        sort(Json.obj("created" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[User]

      // gather all the JsObjects in a list
      val futureUsersList: Future[List[User]] = cursor.toList

      // everything's ok! Let's reply with the array
      futureUsersList.map { persons =>
        Ok(persons.toString)
      }
    }
  }

}