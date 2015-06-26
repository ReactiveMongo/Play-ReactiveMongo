import org.specs2.mutable._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson._

class BSONFormatsSpec extends Specification {
  "BSONFormats" should {
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
        case s          => failure(s"should not be a JsSuccess $s")
      }
    }

    "handle BSONObjectID with toJSON" in {
      val joid2 = Json.obj("$oid" -> "5150806842b329bae81de713", "truc" -> "plop")
      val oid = BSONObjectID.generate
      val joid = Json.toJson(oid)
      toJSON(oid) mustEqual joid
    }

    "handle BSONDateTime" in {
      val dt = BSONDateTime(System.currentTimeMillis())
      val jdt = Json.toJson(dt)

      Json.fromJson[BSONDateTime](jdt) must beLike {
        case JsSuccess(res, _) => res must_== dt
      }
    }

    "bsondoc" in {
      val json = Json.obj(
        "age" -> 4,
        "name" -> "Jack",
        "_id" -> (Json.obj("$oid" -> "5150806842b329bae81de713"): Json.JsValueWrapper),
        "nested" -> Json.arr("plop", 6, Json.obj("toto" -> "titi")))
      val doc = Json.fromJson[BSONDocument](json)

      Json.toJson(doc.get) must_== json
    }

    "handle BSONSymbol" in {
      val symbol = 'sss
      val bsymbol = BSONSymbol(symbol.toString())
      val jsymbol = Json.toJson(bsymbol)
      val bsymbolAgain = Json.fromJson[BSONSymbol](jsymbol)
      bsymbol mustEqual bsymbolAgain.get
    }

    "should convert special Symbol notation" in {
      val symbol = 'sss
      val bsymbol = BSONSymbol(symbol.toString())
      val jsymbol = Json.obj("$symbol" -> symbol.toString)
      Json.fromJson[BSONSymbol](jsymbol).get mustEqual bsymbol
    }

    "should convert special Symbol notation only if there is only one field named $symbol of type String" in {
      val jsymbol = Json.obj("$symbol" -> "sym", "truc" -> "plop")
      Json.fromJson[BSONSymbol](jsymbol) match {
        case JsError(_) => success
        case s          => failure(s"should not be a JsSuccess $s")
      }
    }

    """should convert json regex { "$regex": "^toto", "$options": "i" }""" in {
      val js = Json.obj("$regex" -> "^toto", "$options" -> "i")
      val bson = Json.fromJson[BSONRegex](js).get
      bson mustEqual BSONRegex("^toto", "i")
      val deser = Json.toJson(bson)
      js mustEqual deser
    }

    """should convert json regex { "$options": "i", "$regex": "^toto" }""" in {
      val js = Json.obj("$options" -> "i", "$regex" -> "^toto")
      val bson = Json.fromJson[BSONRegex](js).get
      bson mustEqual BSONRegex("^toto", "i")
      val deser = Json.toJson(bson)
      deser mustEqual Json.obj("$regex" -> "^toto", "$options" -> "i")
    }

    """should convert json regex { "$regex": "^toto" }""" in {
      val js = Json.obj("$regex" -> "^toto")
      val bson = Json.fromJson[BSONRegex](js).get
      bson mustEqual BSONRegex("^toto", "")
      val deser = Json.toJson(bson)
      js mustEqual deser
    }

    """should fail converting json regex { "$options": "i", "$regex": 98 }""" in {
      val js = Json.obj("$options" -> "i", "$regex" -> 98)
      val result = Json.fromJson[BSONRegex](js)
      result.fold(
        x => {
          x.head._1 mustEqual (__ \ "$regex")
        }, x => failure(s"got a JsSuccess = $result instead of a JsError"))
      ok
    }
  }
}
