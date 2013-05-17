import org.specs2.mutable._
import play.api.libs.iteratee._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.FailoverStrategy
import reactivemongo.bson.BSONObjectID
import scala.concurrent._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

class extensions extends Specification {
  import Common._

  import reactivemongo.bson._
  import play.modules.reactivemongo.json.ImplicitBSONHandlers
  import play.modules.reactivemongo.json.ImplicitBSONHandlers._
  import play.modules.reactivemongo.json.BSONFormats._
  import play.modules.reactivemongo.extensions._

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  sequential
  lazy val collectionName = "reactivemongo_test_extensions"
  lazy val bsonCollection = db(collectionName)
  lazy val collection = new JSONCollection(db, collectionName, new FailoverStrategy())

  "String Extensions" should {
    "convert string to BSONObjectID" in {
      val id = BSONObjectID.generate
      val stringId = id.stringify
      stringId.toBSONObjectID mustEqual id
    }

    "slugify string" in {
      val title = "Den här titeln är grym!"
      val slug = title.slug
      slug must beEqualTo("den-har-titeln-ar-grym")
    }

    "slugify and make unique" in {
      val slug1 = "slug"
      slug1.uniqueSlug(Seq(slug1)) must beEqualTo("slug-2")

      val slug2 = "slug-2"
      slug1.uniqueSlug(Seq(slug1, slug2)) must beEqualTo("slug-3")
    }
  }

  "List Extensions" should {
    "convert List to JsArray so that we can return it directly in play's controller" in {

      val futureList = Future.successful(List(1, 2, 3))
      val futureJsArray = futureList.toJsArray

      val jsArray = Await.result(futureJsArray, timeout)

      jsArray must beEqualTo(Json.arr(1, 2, 3))
    }
  }

  "Cursor Extensions" should {
    "convert Cursor to JsArray so that we can return it directly in play's controller" in {

      /*
        Async {
          val usersFuture = collection.find(Json.obj()).cursor[JsObject].toJsArray
          usersFuture.map { users =>
            ok(users)
          }
        }

        or simplified
        Async {
          collection.find(Json.obj()).cursor[JsObject].toJsArray.map(ok(_))
        }
       */

      // Add Some users
      val user = User(Some(BSONObjectID.generate), "John Doe")
      val result = Await.result(collection.save(user), timeout)
      result.ok must beTrue

      val user2 = User(Some(BSONObjectID.generate), "Jane Doe")
      val result2 = Await.result(collection.save(user2), timeout)
      result2.ok must beTrue

      val userJsonArray = Json.arr(
        userWrites.writes(user),
        userWrites.writes(user2))

      val fetched1 = Await.result(collection.find(Json.obj()).cursor[JsObject].toJsArray, timeout)
      fetched1 must beEqualTo(userJsonArray)
    }
  }
}
