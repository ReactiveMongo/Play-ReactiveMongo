import scala.concurrent._
import scala.concurrent.duration._

import reactivemongo.api.AsyncDriver

object Common {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val timeout = 5.seconds
  val timeoutMillis = timeout.toMillis.toInt

  lazy val driver = AsyncDriver()

  lazy val connection =
    Await.result(driver.connect(List("localhost:27017")), timeout)

  lazy val db = Await.result(
    connection.database("specs2-test-reactivemongo").flatMap { _db =>
      _db.drop().map(_ => _db)
    },
    timeout
  )

  def close(): Unit =
    try {
      Await.result(driver.close(), timeout)
    } catch { case _: Throwable => () }
}
