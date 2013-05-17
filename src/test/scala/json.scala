import org.specs2.mutable._
import play.api.libs.iteratee._
import scala.concurrent._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

case class Expeditor(name: String)
case class Item(name: String, description: String, occurrences: Int)
case class Package(
  expeditor: Expeditor,
  items: List[Item],
  price: Float)

class JsonBson extends Specification {
  import Common._

  import reactivemongo.bson._
  import play.modules.reactivemongo.json.ImplicitBSONHandlers
  import play.modules.reactivemongo.json.ImplicitBSONHandlers._
  import play.modules.reactivemongo.json.BSONFormats

  sequential
  lazy val collection = db("somecollection_commonusecases")

  val pack = Package(
    Expeditor("Amazon"),
    List(Item("A Game of Thrones", "Book", 1)),
    20)

  "ReactiveMongo Plugin" should {
    "convert a simple json to bson and vice versa" in {
      val json = Json.obj("coucou" -> JsString("jack"))
      val bson = JsObjectWriter.write(json)
      val json2 = JsObjectReader.read(bson)
      json.toString mustEqual json2.toString
    }
    "convert a simple json array to bson and vice versa" in {
      val json = Json.arr(JsString("jack"), JsNumber(9.1))
      val bson = BSONFormats.toBSON(json).get.asInstanceOf[BSONArray]
      val json2 = BSONFormats.toJSON(bson)
      json.toString mustEqual json2.toString
    }
    "convert a json doc containing an array and vice versa" in {
      val json = Json.obj(
        "name" -> JsString("jack"),
        "contacts" -> Json.arr(
          Json.obj(
            "email" -> "jack@jack.com")))
      val bson = JsObjectWriter.write(json)
      val json2 = JsObjectReader.read(bson)
      json.toString mustEqual json2.toString
    }
  }
}
