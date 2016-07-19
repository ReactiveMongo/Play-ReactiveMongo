import scala.concurrent._
import scala.concurrent.duration._
import reactivemongo.api.MongoDriver

object Common {
  implicit val ec = ExecutionContext.Implicits.global

  val timeout = 5 seconds
  val timeoutMillis = timeout.toMillis.toInt

  lazy val driver = new MongoDriver()
  lazy val connection = driver.connection(List("localhost:27017"))
  lazy val db = Await.result(connection.database("specs2-test-reactivemongo").
    flatMap { _db => _db.drop().map(_ => _db) }, timeout)

  def close(): Unit = try {
    driver.close()
  } catch { case _: Throwable => () }
}
