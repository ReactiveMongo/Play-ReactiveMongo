package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.modules.mongodb._
import play.modules.mongodb.PlayBsonImplicits._
import org.asyncmongo.protocol.messages._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.bson._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.data.validation.Constraints._
import play.api.data.resource._
//import play.api.data.resource.Validators._
import play.modules.mongodb.MongoTemplate
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee._

object Application extends Controller {
  val coll = MongoAsyncPlugin.collection("test")
  val d = new java.util.Date()
  
  def index = Action {
    println("DATE:%d".format(d.getTime + 3))
    val obj = Json.obj(
      "key1" -> "value1", 
      "key2" -> 5, 
      "key3" -> true, 
      "key4" -> Json.arr(1, "bob"),
      "key5" -> Json.obj("$date" -> d.getTime)
    )
    val promise = coll.insert(obj, GetLastError("test")).asPromise
    Async {
  	  promise.map { le => Ok("inserted, GetLastError=" + le.stringify) }
    }
  }
  

  def find = Action {
    import org.asyncmongo.api.Cursor
    import akka.util.Timeout
    import akka.util.duration._

    import play.modules.mongodb.MongoHelpers._

    implicit val timeout = Timeout(5 seconds)

    println("FIND DATE:%d".format(d.getTime))

    val future = coll.find[JsValue, JsValue, JsValue](Json.obj(
      "key5" -> Json.obj(
        "$lt" -> Date(d))), None, 0, 0)

    /*val future = coll.find[JsValue, JsValue, JsValue](Json.obj(
      "_id" -> ObjectId("4fd0511aed79a1989adadd09")), None, 0, 0)*/

    /*val future = coll.find[JsValue, JsValue, JsValue](
      Json.obj("$query" -> Json.obj(
        "key5" -> Json.obj("$date" -> 1339052314301L))), None, 0, 0)*/
"key5" -> ObjectId( "sdfdsfsdfd")
    /*val q = new Bson()
    q.write(BSONDateTime("key5", 1339052314301L))
    val future = coll.find[Bson, JsValue, JsValue](q, None, 0, 0)
    */

    /*Ok.stream(Cursor.enumerate(Some(future)) /*&> Enumeratee.scanLeft("")( _ + "," + _)*/)*/
    //Cursor.enumerate(Some(future)) |>> Iteratee.fold("[")( _ + _) map ( it => Ok(it.run) )
    Ok.stream(Enumerator.flatten((Cursor.enumerate(Some(future)) |>>> Utils.mkString("[", ",", "]"))
      .map(Enumerator(_))).andThen(Enumerator.eof))
  }
}

/*object UserController extends ResourceController(
  Resource[JsValue](MongoTemplate("user"))
    .checking(JsLens \ "name" -> minLength(5))
    .checking(JsLens \ "age" -> max(85))
)*/

object Utils {
  def mkString[E](start: String, sep: String, end: String): Iteratee[E, String] = {

    def step(s: String, isFirst: Boolean)(input: Input[E]): Iteratee[E, String] = {
      input match {
        case Input.EOF => Done(s + end, Input.EOF)

        case Input.Empty => Cont(step(s, isFirst))

        case Input.El(e) => { val s1 = if(isFirst) { s + e.toString } else { s + sep + e.toString }; Cont[E, String](i => step(s1, false)(i)) }
      }
    }

    Cont(step(start, true))

  }
}
