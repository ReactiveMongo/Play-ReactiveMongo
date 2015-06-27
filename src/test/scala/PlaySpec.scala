import com.google.inject

import play.api.inject.guice.GuiceApplicationBuilder

import play.api.test.FakeApplication
import play.api.test.Helpers._

import reactivemongo.api.gridfs.GridFS

import play.modules.reactivemongo.{ DefaultReactiveMongoApi, ReactiveMongoApi }

object PlaySpec extends org.specs2.mutable.Specification {
  "Play integration" title

  "ReactiveMongo API" should {
    "not be resolved if the module is not enabled" in running(
      FakeApplication()) {
        val appBuilder = new GuiceApplicationBuilder().build

        appBuilder.injector.instanceOf[ReactiveMongoApi].
          aka("resolution") must throwA[inject.ConfigurationException]
      }

    "be resolved if the module is enabled" in {
      System.setProperty("config.resource", "test.conf")

      running(FakeApplication()) {
        configuredAppBuilder.injector.instanceOf[ReactiveMongoApi].
          aka("ReactiveMongo API") must beLike {
            case api: DefaultReactiveMongoApi =>
              /* should compile: */ GridFS(api.db)
              ok
          }
      }
    }
  }

  def configuredAppBuilder = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").fold(
      List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().
      configure("play.modules.enabled" -> (modules :+
        "play.modules.reactivemongo.ReactiveMongoModule")).build
  }
}
