# ReactiveMongo Support to Play! Framework 2.0

This is a plugin for Play 2.1, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

## Main features

### JSON <-> BSON conversion

With Play2-ReactiveMongo, you can use directly the embedded JSON library in Play >= 2.1.

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
  "org.reactivemongo" %% "play2-reactivemongo" % "0.9-SNAPSHOT"
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

  // creates a new Person building a JSON from parameters
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
   
  // creates a new Person directly from Json
  def createFromJson = Action(parse.json) {  request =>
    Async {
      collection.insert[JsValue]( request.body ).map( lastError =>
        Ok("Mongo LastErorr:%s".format(lastError))
      )
    }
  }
  
  // queries for a person by name
  def findByName(name: String) = Action {
    Async {
      val qb = QueryBuilder().query(Json.obj( "name" -> name )).sort( "created" -> SortOrder.Descending)

      collection.find[JsValue]( qb ).toList.map { persons =>
        Ok(persons.foldLeft(JsArray(List()))( (obj, person) => obj ++ Json.arr(person) ))
      }
    }
  } 

}
``` 

> Please Notice:
> 
> - your controller may extend `MongoController` which provides a few helpers
> - all actions are asynchronous because ReactiveMongo returns Future[Result]
> - QueryBuilder can be used with Json also



### Helpers for GridFS

Play2-ReactiveMongo makes it easy to serve and store files in a complete non-blocking manner. 
It provides a body parser for handling file uploads, and a method to serve files from a GridFS store.

```scala
def upload = Action(gridFSBodyParser(gridFS)) { request =>
  val future :Future[ReadFileEntry] = request.body.files.head.ref
  // ...
}
```
