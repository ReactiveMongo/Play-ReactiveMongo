import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

class Converters extends Specification {
  "Converters" should {
    "handle BSONObjectID" in {
      val oid = BSONObjectID.generate
      val joid = Json.toJson(oid)
      val oidAgain = Json.fromJson[BSONObjectID](joid)
      oid mustEqual oidAgain.get
    }
    "should convert special ObjectID notation only if there is only one field named $oid of type String" in {
      val joid = Json.obj("$oid" -> "5150806842b329bae81de713", "truc" -> "plop")
      Json.fromJson[BSONObjectID](joid) match {
        case JsError(_) => success
        case success    => failure(s"should not be a JsSuccess $success")
      }
    }
    "handle BSONObjectID with toJSON" in {
      val joid2 = Json.obj("$oid" -> "5150806842b329bae81de713", "truc" -> "plop")
      println(toBSON(joid2))
      val oid = BSONObjectID.generate
      val joid = Json.toJson(oid)
      toJSON(oid) mustEqual joid
    }
    "handle BSONDateTime" in {
      val dt = BSONDateTime(System.currentTimeMillis())
      val jdt = Json.toJson(dt)
      val dtAgain = Json.fromJson[BSONDateTime](jdt)
      dt mustEqual dtAgain.get
    }
    "bsondoc" in {
      val json = Json.obj(
        "age" -> 4,
        "name" -> "Jack",
        "_id" -> Json.obj("$oid" -> "5150806842b329bae81de713"),
        "nested" -> Json.arr("plop", 6, Json.obj("toto" -> "titi")))
      val doc = Json.fromJson[BSONDocument](json)
      val json2 = Json.toJson(doc.get)
      println(doc.map(doc => BSONDocument.pretty(doc)))
      json2 mustEqual json
    }
  }
}