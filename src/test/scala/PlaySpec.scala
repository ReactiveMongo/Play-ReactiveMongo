import com.google.inject

import scala.concurrent.{ ExecutionContext, Future }

import play.api.inject.guice.GuiceApplicationBuilder

import play.api.test.FakeApplication
import play.api.test.Helpers.running

import play.modules.reactivemongo.{
  NamedDatabase,
  ReactiveMongoApiFromContext,
  ReactiveMongoApi,
  TestUtils
}

import org.specs2.concurrent.{ ExecutionEnv => EE }

class PlaySpec extends org.specs2.mutable.Specification {
  "Play integration" title

  sequential

  import Common.timeout

  "ReactiveMongo API" should {
    "not be resolved if the module is not enabled" in {
      running(FakeApplication()) {
        val appBuilder = new GuiceApplicationBuilder().build

        appBuilder.injector.instanceOf[ReactiveMongoApi].
          aka("resolution") must throwA[inject.ConfigurationException]
      }
    }

    "be resolved" >> {
      "as default instance if the module is enabled" in { implicit ee: EE =>
        System.setProperty("config.resource", "test1.conf")

        running(configure _) {
          _.injector.instanceOf[ReactiveMongoApi].
            database.map(_.name) must beEqualTo("test1").await(0, timeout)
        }
      }

      "as multiple instances if the module is enabled" in { implicit ee: EE =>
        System.setProperty("config.resource", "test3.conf")

        running(FakeApplication()) {
          val names = Seq("default", "bar", "lorem", "ipsum")

          Future.sequence(names.map { name =>
            configuredAppBuilder.injector.instanceOf[ReactiveMongoApi](
              TestUtils.bindingKey(name)
            ).database.map(_.name)
          }) aka "DB names" must contain(allOf("foo", "bar", "lorem", "ipsum")).
            await(0, timeout)
        }
      }
    }

    "be injected" >> {
      "as default instance" in { implicit ee: EE =>
        System.setProperty("config.resource", "test1.conf")

        running(configure _) { app =>
          app.injector.instanceOf[InjectDefault].database.
            aka("DB name") must beEqualTo("test1").await(1, timeout)
        }
      }

      "as instance named 'default'" in { implicit ee: EE =>
        System.setProperty("config.resource", "test1.conf")

        running(configure _) { app =>
          app.injector.instanceOf[InjectDefaultNamed].database.
            aka("DB name") must beEqualTo("test1").await(1, timeout)
        }
      }

      "as instance named 'foo'" in { implicit ee: EE =>
        System.setProperty("config.resource", "test2.conf")

        running() {
          _.injector.instanceOf[InjectFooNamed].database.
            aka("DB name") must beEqualTo("test2").await(1, timeout)
        }
      }

      "as multiple default and named instance" in { implicit ee: EE =>
        System.setProperty("config.resource", "test3.conf")

        running(configure _) {
          _.injector.instanceOf[InjectMultiple].databases.
            aka("DB names") must beEqualTo(
              ("foo", "foo", "bar", "lorem", "ipsum")
            ).await(1, timeout)
        }
      }
    }

    "be initialized from custom application context" >> {
      def reactiveMongoApi(n: String = "default") = {
        import play.api.{ ApplicationLoader, Configuration }

        val env = play.api.Environment.simple(mode = play.api.Mode.Test)
        val config = Configuration.load(env)

        val context = ApplicationLoader.Context(env, None,
          new play.core.DefaultWebCommands(), config)

        val apiFromCustomCtx = new ReactiveMongoApiFromContext(context, n) {
          lazy val router = play.api.routing.Router.empty
        }

        apiFromCustomCtx.reactiveMongoApi
      }

      "successfully with default non-strict URI" in { implicit ee: EE =>
        System.setProperty("config.resource", "test1.conf")

        reactiveMongoApi().database.map(_.name).
          aka("DB resolution") must beEqualTo("test1").await(0, timeout)
      }

      "successfully with other non-strict URI" in { implicit ee: EE =>
        System.setProperty("config.resource", "test2.conf")

        reactiveMongoApi("foo").database.map(_.name).
          aka("DB name") must beEqualTo("test2").await(0, timeout)
      }

      "successfully from composite configuration" in { implicit ee: EE =>
        System.setProperty("config.resource", "test3.conf")

        (for {
          _1 <- reactiveMongoApi().database.map(_.name)
          _2 <- reactiveMongoApi("default").database.map(_.name)
          _3 <- reactiveMongoApi("bar").database.map(_.name)
          _4 <- reactiveMongoApi("lorem").database.map(_.name)
          _5 <- reactiveMongoApi("ipsum").database.map(_.name)
        } yield (_1, _2, _3, _4, _5)).aka("DB names") must beTypedEqualTo(
          ("foo", "foo", "bar", "lorem", "ipsum")
        ).await(0, timeout)
      }

      "successfully with non-strict URI with prefix" in { implicit ee: EE =>
        System.setProperty("config.resource", "test4.conf")

        reactiveMongoApi("default").database.map(_.name).
          aka("DB name") must beEqualTo("test4").await(0, timeout)
      }

      "successfully with strict URI" in { implicit ee: EE =>
        System.setProperty("config.resource", "strict1.conf")
        reactiveMongoApi().database.map(_.name).
          aka("DB name") must beEqualTo("strict1").await(0, timeout)
      }

      "and failed with strict URI and unsupported option" in {
        implicit ee: EE =>
          System.setProperty("config.resource", "strict2.conf")
          reactiveMongoApi().database.map(_ => {}).
            aka("DB resolution") must throwA[IllegalArgumentException](
              "The connection URI contains unsupported options: foo"
            )

      }
    }
  }

  // ---

  import scala.collection.JavaConversions.iterableAsScalaIterable

  def configuredAppBuilder = {
    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").fold(
      List.empty[String]
    )(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().
      configure("play.modules.enabled" -> (modules :+
        "play.modules.reactivemongo.ReactiveMongoModule")).build
  }

  def configure(initial: GuiceApplicationBuilder): GuiceApplicationBuilder = {
    initial.load(
      new play.api.inject.BuiltinModule(),
      new play.modules.reactivemongo.ReactiveMongoModule()
    )
  }
}

import javax.inject.Inject

class InjectDefault @Inject() (api: ReactiveMongoApi) {
  def database(implicit ec: ExecutionContext): Future[String] =
    api.database.map(_.name)
}

class InjectDefaultNamed @Inject() (
    @NamedDatabase("default") api: ReactiveMongoApi
) {
  def database(implicit ec: ExecutionContext): Future[String] =
    api.database.map(_.name)
}

class InjectFooNamed @Inject() (
    @NamedDatabase("foo") api: ReactiveMongoApi
) {
  def database(implicit ec: ExecutionContext): Future[String] =
    api.database.map(_.name)
}

class InjectMultiple @Inject() (
    defaultApi: ReactiveMongoApi,
    @NamedDatabase("default") namedDefault: ReactiveMongoApi,
    @NamedDatabase("bar") bar: ReactiveMongoApi,
    @NamedDatabase("lorem") lorem: ReactiveMongoApi,
    @NamedDatabase("ipsum") ipsum: ReactiveMongoApi
) {
  def databases(implicit ec: ExecutionContext): Future[(String, String, String, String, String)] = for {
    a <- defaultApi.database.map(_.name)
    b <- namedDefault.database.map(_.name)
    c <- bar.database.map(_.name)
    d <- lorem.database.map(_.name)
    e <- ipsum.database.map(_.name)
  } yield (a, b, c, d, e)
}
