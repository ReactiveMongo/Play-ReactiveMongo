import org.specs2.mutable._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.modules.reactivemongo.MongoCollection
import reactivemongo.bson.BSONObjectID
import scala.concurrent._
import play.modules.reactivemongo.extensions._
import Common._

case class Post(
  title: String,
  content: String,
  active: Boolean = true,
  id: Option[BSONObjectID] = None
) {
  val slug = title.slug
}

object Post {
  import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

  implicit val format = Json.format[Post]

  implicit object collection extends MongoCollection[Post] {
    // The collection will get it's name from the lower case name of the case class plus an "s" for plural, you can override it if you want.
    //override def name = "posts"

    // Only needed for test, since it picks up the db from the plugin
    override def db = Common.db
  }
}

class MongoCollectionSpec extends Specification {

  sequential

  "MongoCollectionSpec" should {

    "Create posts" in {
      import Post._
      val posts = Seq(
        Post("This is the first post", "Hello from post 1"),
        Post("This is a post with something else", "Hello from post 2"),
        Post("This post is inactive", "I'm inactive", active = false))

      val inserted = Await.result(Post.collection.bulkInsert(Enumerator.enumerate(posts)), timeout)
      inserted must beEqualTo(3)
    }

    "Query for active posts" in {
      val activePosts = Await.result(Post.collection.query("active" -> true), timeout)
      activePosts.size must beEqualTo(2)
    }
  }

}
