import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.modules.mongodb._
import play.modules.mongodb.MONGO
import play.api.libs.json._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.modules.mongodb.Parsers._

@RunWith(classOf[JUnitRunner])
class MongoSpec extends Specification {
  "Mongo" should {
    "query simple json" in {
      running(FakeApplication(
          additionalConfiguration= Map( 
              "mongodb.db" -> "test", 
              "mongodb.host" -> "localhost"))) {
        import play.api.Play.current
        import play.modules.mongodb.MongoImplicits._
        import play.tools.richjson.RichJson._

        val coll = MongoPlugin.collection("test")
        implicit val qe = MongoQueryExecutor(coll)

        val q = MONGO("name" \: "bob")
        val res = q().map { e =>
          e[String]("name") -> e[Int]("age")
        }.toList
        
        println("RES:" + res)

        val res2 = MONGO("name" \: "bart").as(json single) \ "address" \ "street"
        println("RES2:" + res2)

        val res3 = MONGO().as( str("name") ~ int("age") map(flatten) *)
        println("RES3:" + res3)

        val res4 = MONGO("name" \: "bart").as(json("address") single)
        println("RES4:" + res4)

        success
      }
    }
  }
}