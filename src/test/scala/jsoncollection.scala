import org.specs2.mutable._
import play.api.libs.iteratee._
import scala.concurrent._

import reactivemongo.bson.BSONObjectID

case class User(
  _id: Option[BSONObjectID] = None,
  username: String)

class JSONCollectionSpec extends Specification {
  import Common._

  import reactivemongo.bson._
  import reactivemongo.api.FailoverStrategy
  import play.modules.reactivemongo.json.BSONFormats._
  import play.modules.reactivemongo.json.collection.JSONCollection

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  sequential
  lazy val collectionName = "reactivemongo_test_users"
  lazy val bsonCollection = db(collectionName)
  lazy val collection = new JSONCollection(db, collectionName, new FailoverStrategy())

  "JSONCollection.save" should {

    "add object if there does not exist in database" in {
      // Check current document does not exist
      val query = BSONDocument("username" -> BSONString("John Doe"))
      val fetched1 = Await.result(bsonCollection.find(query).one, timeout)
      fetched1 must beNone

      // Add document..
      val user = User(username = "John Doe")
      val result = Await.result(collection.save(user), timeout)
      result.ok must beTrue

      // Check data in mongodb..
      val fetched2 = Await.result(bsonCollection.find(query).one, timeout)
      fetched2 must beSome.like {
        case d: BSONDocument => {
          d.get("_id").isDefined must beTrue
          d.get("username") must beSome(BSONString("John Doe"))
        }
      }
    }

    "update object there already exists in database" in {
      // Find saved object
      val fetched1 = Await.result(collection.find(Json.obj("username" -> "John Doe")).one[User], timeout)
      fetched1 must beSome.like {
        case u: User => {
          u._id.isDefined must beTrue
          u.username must beEqualTo("John Doe")
        }
      }

      // Update object..
      val newUser = fetched1.get.copy(username = "Jane Doe")
      val result = Await.result(collection.save(newUser), timeout)
      result.ok must beTrue

      // Check data in mongodb..
      val fetched2 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("John Doe"))).one, timeout)
      fetched2 must beNone

      val fetched3 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("Jane Doe"))).one, timeout)
      fetched3 must beSome.like {
        case d: BSONDocument => {
          d.get("_id") must beSome(fetched1.get._id.get)
          d.get("username") must beSome(BSONString("Jane Doe"))
        }
      }
    }

    "add object if there does not exist but its field `_id` is setted" in {
      // Check current document does not exist
      val query = BSONDocument("username" -> BSONString("Robert Roe"))
      val fetched1 = Await.result(bsonCollection.find(query).one, timeout)
      fetched1 must beNone

      // Add document..
      val id = BSONObjectID.generate
      val user = User(_id = Option(id), username = "Robert Roe")
      val result = Await.result(collection.save(user), timeout)
      result.ok must beTrue

      // Check data in mongodb..
      val fetched2 = Await.result(bsonCollection.find(query).one, timeout)
      fetched2 must beSome.like {
        case d: BSONDocument => {
          d.get("_id") must beSome(id)
          d.get("username") must beSome(BSONString("Robert Roe"))
        }
      }
    }

  }

}
