package controllers

import org.asyncmongo.bson._
import play.api.libs.json._
import play.api.libs.json.Constraints._
import play.api.mvc._
import play.modules.mongodb._
import play.api.Play.current
import org.asyncmongo.protocol.messages._
import play.modules.mongodb.PlayBsonImplicits._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.bson._
import play.api.libs.concurrent._


object User extends Controller {
	val users = MongoAsyncPlugin.collection("users")

  //case class User(name: String, email: String, password: string, age: Option[Int])
  implicit val userFormat: Format[(String, String, String, Option[Int])] = JsTupler(
    JsPath \ 'name -> in( required[String] ),
    JsPath \ 'email -> in( required[String] and email ),
    JsPath \ 'password -> (in( required[String] ) ~ out( pruned[String] )),
    JsPath \ 'age -> in( optional[Int] )
  )

	def register = Action { implicit request => Async {
		request.body.asJson.map { json =>
    	json.validate[(String, String, String, Option[Int])].fold( 
        valid = { user => 
          val promise = users.insert(json, GetLastError(MongoAsyncPlugin.dbName)).asPromise
          promise.map { le => Ok("Created user " + Json.toJson(user)) }
        },
        invalid = jserror => Promise.pure(BadRequest("validation error:%s".format(jserror.toString)))
      )
    }.getOrElse {
      Promise.pure(BadRequest("Expecting Json data"))
    }
  }}


  implicit val addressFormat: Format[(String, Int, String)] = JsTupler(
    JsPath \ 'street -> in(required[String]),
    JsPath \ 'nb -> in(required[Int]),
    JsPath \ 'town -> in[String]
  ) 

  implicit val user2Format: Format[(String, String, (String, Int, String))] = JsTupler(
    JsPath \ 'name -> in( required[String] ),
    JsPath \ 'email -> in( required[String] and email ),
    JsPath \ 'address -> addressFormat
  )

  def register2 = Action { implicit request =>
    request.body.asJson.map { json =>
      json.validate[(String, String, (String, Int, String))].fold( 
        valid = user => 
          Ok("Hello " + Json.toJson(user)),
        invalid = jserror => BadRequest("validation error:%s".format(jserror.toString))
      )
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
  }
}