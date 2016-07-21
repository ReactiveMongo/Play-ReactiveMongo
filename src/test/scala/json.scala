import play.api.libs.json._
import play.api.libs.json.Reads._

case class Expeditor(name: String)
case class Item(name: String, description: String, occurrences: Int)
case class Package(
  expeditor: Expeditor,
  items: List[Item],
  price: Float
)

class JsonBson extends org.specs2.mutable.Specification {
  import Common._

  import reactivemongo.bson._
  import play.modules.reactivemongo.json._

  sequential
  lazy val collection = db("somecollection_commonusecases")

  val pack = Package(
    Expeditor("Amazon"),
    List(Item("A Game of Thrones", "Book", 1)),
    20
  )

  "ReactiveMongo Plugin" should {
    "convert an empty json and give an empty bson doc" in {
      val json = Json.obj()
      val bson = BSONFormats.toBSON(json).get.asInstanceOf[BSONDocument]
      bson.isEmpty mustEqual true
      val json2 = BSONFormats.toJSON(bson)
      json2.as[JsObject].value.size mustEqual 0
    }
    "convert a simple json to bson and vice versa" in {
      val json = Json.obj("coucou" -> JsString("jack"))
      val bson = JsObjectWriter.write(json)
      val json2 = JsObjectReader.read(bson)
      json.toString mustEqual json2.toString
    }
    "convert a simple json array to bson and vice versa" in {
      val json = Json.arr(JsString("jack"), JsNumber(9.1))
      val bson = BSONFormats.toBSON(json).get.asInstanceOf[BSONArray]

      json.toString mustEqual BSONFormats.toJSON(bson).toString
    }
    "convert a json doc containing an array and vice versa" in {
      val json = Json.obj(
        "name" -> JsString("jack"),
        "contacts" -> Json.arr(Json.toJsFieldJsValueWrapper(Json.obj("email" -> "jack@jack.com")))
      )
      val bson = JsObjectWriter.write(json)

      json.toString mustEqual JsObjectReader.read(bson).toString
    }

    "format a jspath for mongo crud" in {
      import play.api.libs.functional.syntax._
      import play.modules.reactivemongo.json.Writers._

      case class Limit(low: Option[Int], high: Option[Int])
      case class App(limit: Option[Limit])

      val lowWriter = (__ \ "low").writeNullable[Int]
      val highWriter = (__ \ "high").writeNullable[Int]
      val limitWriter = (lowWriter and highWriter)(unlift(Limit.unapply))
      val appWriter = (__ \ "limit").writemongo[Limit](limitWriter)

      appWriter.writes(Limit(Some(1), None)) mustEqual
        Json.obj("limit.low" -> 1)
      appWriter.writes(Limit(Some(1), Some(2))) mustEqual
        Json.obj("limit.low" -> 1, "limit.high" -> 2)
      appWriter.writes(Limit(None, None)) mustEqual
        Json.obj()
    }

    "convert JSON numbers to BSON" >> {
      "as double for 1.0" in {
        BSONFormats.toBSON(JsNumber(BigDecimal("1.0"))).
          aka("integer as decimal") must_== JsSuccess(BSONDouble(1.0D))
      }

      "as double for 1.5" in {
        BSONFormats.toBSON(JsNumber(BigDecimal("1.5"))).
          aka("decimal number") must_== JsSuccess(BSONDouble(1.5D))
      }

      "as long for Long.MaxValue (doesn't fit Int)" in {
        BSONFormats.toBSON(JsNumber(BigDecimal(Long.MaxValue))).
          aka("long integer") must_== JsSuccess(BSONLong(Long.MaxValue))
      }

      "as integer for 1" in {
        BSONFormats.toBSON(JsNumber(BigDecimal("1"))).
          aka("integer") must_== JsSuccess(BSONInteger(1))
      }
    }
  }
}
