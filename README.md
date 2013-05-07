# ReactiveMongo Support to Play! Framework 2.1

This is a plugin for Play 2.1, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

## Main features

### JSON <-> BSON conversion

With Play2-ReactiveMongo, you can use directly the embedded JSON library in Play >= 2.1. There is a specialized collection called `JSONCollection` that deals naturally with `JSValue` and `JSObject` instead of ReactiveMongo's `BSONDocument`.

The JSON lib has been completely refactored and is now the most powerful one in the Scala world. Thanks to it, you can now fetch documents from MongoDB in the JSON format, transform them by removing and/or adding some properties, and send them to the client. Even better, when a client sends a JSON document, you can validate it and transform it before saving it into a MongoDB collection.

Another advantage to use this plugin is to be capable of using JSON documents for querying MongoDB.


### Add ReactiveMongo to your dependencies

In your project/Build.scala:

```scala
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
)
```

If you want to use the latest snapshot, add the following instead:

```scala
resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10-SNAPSHOT"
)
```

### Configure your application to use ReactiveMongo plugin

#### add to your conf/play.plugins

``` 
400:play.modules.reactivemongo.ReactiveMongoPlugin
```


### Configure your database access within `application.conf`

This plugin reads connection properties from the `application.conf` and gives you an easy access to the connected database.

#### add this to your conf/application.conf

```
mongodb.servers = ["localhost:27017"]
mongodb.db = "your_db_name"
```
alternatively, you can use the URI syntax to point to your MongoDB:
```
mongodb.uri ="mongodb://username:password@localhost:27017/your_db_name"
```
This is especially helpful on platforms like Heroku, where add-ons publish the connection URI in a single environment variable. The URI syntax supports the following format: `mongodb://[username:password@]host1[:port1][,hostN[:portN]]/dbName`

### Play2 controller sample

```scala
package controllers

import play.api._
import play.api.mvc._

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection

// Play Json imports
import play.api.libs.json._

import play.api.Play.current

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
  
  // queries for a person by name
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

}
``` 

> Please Notice:
> 
> - your controller may extend `MongoController` which provides a few helpers
> - all actions are asynchronous because ReactiveMongo returns `Future[Result]`
> - we use a specialized collection called `JSONCollection` that deals naturally with `JSValue` and `JSObject`

### Play2 controller sample using Json Writes and Reads

First, the models:

```scala
package models

case class User(
  age: Int,
  firstName: String,
  lastName: String,
  feeds: List[Feed])

case class Feed(
  name: String,
  url: String)

object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val feedFormat = Json.format[Feed]
  implicit val userFormat = Json.format[User]
}
```

Then, the controller which uses the ability of the `JSONCollection` to handle Json's `Reads` and `Writes`:

```scala
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
 * Example using ReactiveMongo + Play JSON library,
 * using case classes that can be turned into Json using Reads and Writes.
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
```


### Helpers for GridFS

Play2-ReactiveMongo makes it easy to serve and store files in a complete non-blocking manner. 
It provides a body parser for handling file uploads, and a method to serve files from a GridFS store.

```scala
def upload = Action(gridFSBodyParser(gridFS)) { request =>
  // here is the future file!
  val futureFile: Future[ReadFile[BSONValue]] = request.body.files.head.ref
  futureFile.map { file =>
    // do something
    Ok
  }.recover {
    case e: Throwable => InternalServerError(e.getMessage)
  }
}
```
