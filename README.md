# ReactiveMongo Support to Play! Framework 2.4

This is a plugin for Play 2.4, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

## ReactiveMongoPlugin is deprecated

Play has deprecated plugins in version 2.4. Therefore it is recommended to remove it from your project and replace it by
ReactiveMongoModule which configures dependency injection and ReactiveMongoApi which is the interface to MongoDB.

## Use ReactiveMongoModule and ReactiveMongoApi

Add following line to `application.conf`:

```scala
modules.enabled += "play.modules.reactivemongo.ReactiveMongoModule"
```

Then use Play's dependency injection mechanism to resolve instance of `ReactiveMongoApi` which is the interface to MongoDB. Example:

```scala
class MyRepository @Inject() (reactiveMongoApi: ReactiveMongoApi) {
  ...
  lazy val db = reactiveMongoApi.db
  ...
}
```

## Main features

### JSON <-> BSON conversion

With Play2-ReactiveMongo, you can use directly the embedded JSON library in Play >= 2.1. There is a specialized collection called `JSONCollection` that deals naturally with `JSValue` and `JSObject` instead of ReactiveMongo's `BSONDocument`.

The JSON lib has been completely refactored and is now the most powerful one in the Scala world. Thanks to it, you can now fetch documents from MongoDB in the JSON format, transform them by removing and/or adding some properties, and send them to the client. Even better, when a client sends a JSON document, you can validate it and transform it before saving it into a MongoDB collection.

Another advantage to use this plugin is to be capable of using JSON documents for querying MongoDB.


### Add ReactiveMongo to your dependencies

In your project/Build.scala:

```scala
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23.play24-SNAPSHOT"
)
```

If you want to use the latest snapshot, add the following instead:

```scala
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.0-SNAPSHOT"
)
```

### Configure your application to use ReactiveMongo plugin

#### add to your conf/play.plugins

```
1100:play.modules.reactivemongo.ReactiveMongoPlugin
```


### Configure your database access within `application.conf`

This plugin reads connection properties from the `application.conf` and gives you an easy access to the connected database.

#### add this to your conf/application.conf

You can use the URI syntax to point to your MongoDB:

```
mongodb.uri = "mongodb://someuser:somepasswd@localhost:27017/your_db_name"
```

or, alternatively:

```
mongodb = {
  db = "your_db_name"
  servers = [ "localhost:27017" ]
  credentials = {
    username = "someuser"
    password = "somepasswd"
  }
}
```


This is especially helpful on platforms like Heroku, where add-ons publish the connection URI in a single environment variable. The URI syntax supports the following format: `mongodb://[username:password@]host1[:port1][,hostN[:portN]]/dbName?option1=value1&option2=value2`

A more complete example:

```
# Either the URI form (preferred)
mongodb.uri = "mongodb://someuser:somepasswd@host1:27017,host2:27017,host3:27017/your_db_name?authSource=authdb&rm.nbChannelsPerNode=10"

# Or, the legacy way:
mongodb = {
  db = "your_db_name"
  servers = [ "host1:27017", "host2:27017", "host3:27017" ]
  options = {
    nbChannelsPerNode = 10
    authSource = "authdb"
  }
  credentials = {
    username = "someuser"
    password = "somepasswd"
  }
}

# If both are present, only the URI form will be parsed.
```

### Configure underlying akka system

ReactiveMongo loads its configuration from the key `mongo-async-driver`

To change the log level (prevent dead-letter logging for example)

```
mongo-async-driver {
  akka {
    loglevel = WARNING
  }
}
```

### Play2 controller sample

```scala
package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
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
 * This controller uses JsObjects directly.
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

  def create(name: String, age: Int) = Action.async {
    val json = Json.obj(
      "name" -> name,
      "age" -> age,
      "created" -> new java.util.Date().getTime())

    collection.insert(json).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }

  def createFromJson = Action.async(parse.json) { request =>
    import play.api.libs.json.Reads._
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
    val transformer: Reads[JsObject] =
      Reads.jsPickBranch[JsString](__ \ "firstName") and
        Reads.jsPickBranch[JsString](__ \ "lastName") and
        Reads.jsPickBranch[JsNumber](__ \ "age") reduce

    request.body.transform(transformer).map { result =>
      collection.insert(result).map { lastError =>
        Logger.debug(s"Successfully inserted with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findByName(name: String) = Action.async {
    // let's do our query
    val cursor: Cursor[JsObject] = collection.
      // find all people with name `name`
      find(Json.obj("name" -> name)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[JsObject]

    // gather all the JsObjects in a list
    val futurePersonsList: Future[List[JsObject]] = cursor.collect[List]()

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
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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
 * This controller uses case classes and their associated Reads/Writes
 * to read or write JSON structures.
 *
 * Instead of using the default Collection implementation (which interacts with
 * BSON structures + BSONReader/BSONWriter), we use a specialized
 * implementation that works with JsObject + Reads/Writes.
 *
 * Of course, you can still use the default Collection implementation
 * (BSONCollection.) See ReactiveMongo examples to learn how to use it.
 */
object ApplicationUsingJsonReadersWriters extends Controller with MongoController {
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

  def create = Action.async {
    val user = User(29, "John", "Smith", List(
      Feed("Slashdot news", "http://slashdot.org/slashdot.rdf")))
    // insert the user
    val futureResult = collection.insert(user)
    // when the insert is performed, send a OK 200 result
    futureResult.map(_ => Ok)
  }

  def createFromJson = Action.async(parse.json) { request =>
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
    request.body.validate[User].map { user =>
      // `user` is an instance of the case class `models.User`
      collection.insert(user).map { lastError =>
        Logger.debug(s"Successfully inserted with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findByName(lastName: String) = Action.async {
    // let's do our query
    val cursor: Cursor[User] = collection.
      // find all people with name `name`
      find(Json.obj("lastName" -> lastName)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[User]

    // gather all the JsObjects in a list
    val futureUsersList: Future[List[User]] = cursor.collect[List]()

    // everything's ok! Let's reply with the array
    futureUsersList.map { persons =>
      Ok(persons.toString)
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
