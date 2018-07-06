package play.modules.reactivemongo

import java.util.UUID

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import reactivemongo.api.DefaultDB
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global

class InjectionSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with Eventually {

  private val testDbName = s"test-${this.getClass.getSimpleName}"
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(150, Millis))

  override lazy val app =
    new GuiceApplicationBuilder()
      .bindings(new ReactiveMongoHmrcModule)
      .configure(Configuration("mongodb.uri" -> s"mongodb://localhost:27017/$testDbName"))
      .build()

  "Mongo" should {
    "work when ReactiveMongoComponent was injected" in {
      val component = app.injector.instanceOf[ReactiveMongoComponent]
      verify(component.mongoConnector.db)
    }
  }

  def verify(db: () => DefaultDB): Unit = {
    val collection = db().collection[JSONCollection]("test-collection")
    val doc        = Json.obj("_id" -> UUID.randomUUID().toString)

    collection.insert(doc).futureValue
    collection.find(doc).one[JsValue].futureValue shouldBe 'defined
  }

}
