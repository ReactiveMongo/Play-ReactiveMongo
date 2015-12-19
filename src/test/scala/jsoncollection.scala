import reactivemongo.core.errors.DetailedDatabaseException

import scala.concurrent._, duration._

object JSONCollectionSpec extends org.specs2.mutable.Specification {
  "JSON collection" title

  sequential

  import Common._
  import play.api.libs.json.{ JsObject, _ }
  import play.modules.reactivemongo.json._
  import play.modules.reactivemongo.json.collection.{ JSONCollection, JSONQueryBuilder }
  import reactivemongo.api.commands.WriteResult
  import reactivemongo.api.{ FailoverStrategy, ReadPreference }
  import reactivemongo.bson._

  case class User(
    _id: Option[BSONObjectID] = None, username: String, height: Double)

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  lazy val collectionName = "reactivemongo_test_users"
  lazy val bsonCollection = db(collectionName)
  lazy val collection = new JSONCollection(db, collectionName, new FailoverStrategy())

  "JSONCollection.save" should {
    "add object if there does not exist in database" in {
      // Check current document does not exist
      val query = BSONDocument("username" -> BSONString("John Doe"))
      bsonCollection.find(query).one[JsObject] must beNone.await(timeoutMillis)

      // Add document..
      collection.save(User(username = "John Doe", height = 12)).
        aka("save") must beLike[WriteResult] {
          case result => result.ok must beTrue
        }.await(timeoutMillis)

      // Check data in mongodb..
      bsonCollection.find(query).one[BSONDocument].
        aka("result") must beSome[BSONDocument].which { d =>
          d.get("_id") must beSome and (
            d.get("username") must beSome(BSONString("John Doe")))
        }.await(timeoutMillis)
    }

    "update object there already exists in database" in {
      // Find saved object
      val fetched1 = Await.result(collection.find(Json.obj("username" -> "John Doe")).one[User], timeout)
      fetched1 must beSome[User].which { u =>
        u._id.isDefined must beTrue and (u.username must_== "John Doe")
      }

      // Update object..
      val newUser = fetched1.get.copy(username = "Jane Doe")
      val result = Await.result(collection.save(newUser), timeout)
      result.ok must beTrue

      // Check data in mongodb..
      val fetched2 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("John Doe"))).one[BSONDocument], timeout)
      fetched2 must beNone

      val fetched3 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("Jane Doe"))).one[BSONDocument], timeout)
      fetched3 must beSome[BSONDocument].which { d =>
        d.get("_id") must beSome(fetched1.get._id.get) and (
          d.get("username") must beSome(BSONString("Jane Doe")))
      }
    }

    "add object if does not exist but its field `_id` is setted" in {
      // Check current document does not exist
      val query = BSONDocument("username" -> BSONString("Robert Roe"))
      bsonCollection.find(query).one[BSONDocument].
        aka("result") must beNone.await(timeoutMillis)

      // Add document..
      val id = BSONObjectID.generate
      collection.save(User(
        _id = Some(id), username = "Robert Roe", height = 13)).
        aka("save") must beLike[WriteResult] {
          case result => result.ok must beTrue
        }.await(timeoutMillis)

      // Check data in mongodb..
      bsonCollection.find(query).one[BSONDocument].
        aka("result") must beSome[BSONDocument].which { d =>
          d.get("_id") must beSome(id) and (
            d.get("username") must beSome(BSONString("Robert Roe")))
        }.await(timeoutMillis)
    }
  }

  "JSONCollection.findAndModify" should {
    "be successful" in {
      val id = BSONObjectID.generate
      val updateOp = collection.updateModifier(User(_id = Some(id),
        username = "James Joyce", height = 1.264290338792695E+64),
        fetchNewObject = false, upsert = true)

      collection.findAndModify(BSONDocument("_id" -> id), updateOp).
        map(_.result[BSONDocument]) must beNone.await(timeoutMillis)
    }
  }

  "JSONQueryBuilder.merge" should {
    "write an JsObject with mongo query only if there are not options defined" in {
      val builder = JSONQueryBuilder(
        collection = collection,
        failover = new FailoverStrategy(),
        queryOption = Option(Json.obj("username" -> "John Doe")))

      builder.merge(ReadPreference.Primary).toString.
        aka("merged") must beEqualTo("{\"username\":\"John Doe\"}")
    }

    "write an JsObject with only defined options" in {
      val builder1 = JSONQueryBuilder(
        collection = collection,
        failover = new FailoverStrategy(),
        queryOption = Option(Json.obj("username" -> "John Doe")),
        sortOption = Option(Json.obj("age" -> 1)))
      builder1.merge(ReadPreference.Primary).toString must beEqualTo("{\"$query\":{\"username\":\"John Doe\"},\"$orderby\":{\"age\":1}}")

      val builder2 = builder1.copy(commentString = Option("get john doe users sorted by age"))
      builder2.merge(ReadPreference.Primary).toString must beEqualTo("{\"$query\":{\"username\":\"John Doe\"},\"$orderby\":{\"age\":1},\"$comment\":\"get john doe users sorted by age\"}")
    }
  }

  "JSON collection" should {
    "find with empty criteria document" in {
      collection.find(Json.obj(
        "$query" -> Json.obj(), "$orderby" -> Json.obj("updated" -> -1))).
        aka("find with empty document") must not(throwA[Throwable])
    }

    "count all matching document" in {
      collection.count() aka "all" must beEqualTo(3).await(timeoutMillis) and (
        collection.count(Some(Json.obj("username" -> "Jane Doe"))).
        aka("with query") must beEqualTo(1).await(timeoutMillis)) and (
          collection.count(limit = 1) aka "limited" must beEqualTo(1).
          await(timeoutMillis))
    }
  }

  "JSON cursor" should {
    "return result as a JSON array" in {
      import play.modules.reactivemongo.json.collection.JsCursor._

      collection.find(Json.obj()).cursor[JsObject]().jsArray().
        map(_.value.map { js => (js \ "username").as[String] }).
        aka("extracted JSON array") must beEqualTo(List(
          "Jane Doe", "Robert Roe", "James Joyce")).await(timeoutMillis)
    }

    "fail on maxTimeout" in {
      Await.ready(Future.sequence {
        for (i <- 1 to 100000)
          yield collection.insert(Json.obj("doc" -> s"doc-$i"))
      }, DurationInt(60).seconds)

      Await.result(collection.find(Json.obj("doc" -> "docX")).maxTimeMs(1).cursor[JsValue]().collect[List](10), DurationInt(1).second) must throwA[DetailedDatabaseException]
    }
  }
}
