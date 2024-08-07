import scala.concurrent.{ ExecutionContext, Future }

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import play.modules.reactivemongo.{
  CollectionResolution,
  NamedDatabase,
  ReactiveMongoApi,
  ReactiveMongoApiFromContext,
  TestUtils,
  WithCollection
}

import org.specs2.concurrent.ExecutionEnv

import com.google.inject

final class PlaySpec(
    implicit
    ee: ExecutionEnv)
    extends org.specs2.mutable.Specification {

  "Play integration".title

  sequential

  import Common.timeout
  import PlayUtil.configure

  "ReactiveMongo API" should {
    "not be resolved if the module is not enabled" in {
      val appBuilder = new GuiceApplicationBuilder().build()

      appBuilder.injector
        .instanceOf[ReactiveMongoApi]
        .aka("resolution") must throwA[inject.ConfigurationException]
    }

    "be resolved" >> {
      "as default instance if the module is enabled" in {
        System.setProperty("config.resource", "test1.conf")

        running(configure _) {
          _.injector
            .instanceOf[ReactiveMongoApi]
            .database
            .map(_.name) must beEqualTo("test1").await(0, timeout)
        }
      }

      "as multiple instances if the module is enabled" in {
        System.setProperty("config.resource", "test3.conf")

        val names = Seq("default", "bar", "lorem", "ipsum")

        Future.sequence(names.map { name =>
          configuredAppBuilder.injector
            .instanceOf[ReactiveMongoApi](
              TestUtils.bindingKey(name)
            )
            .database
            .map(_.name)
        }) aka "DB names" must contain(allOf("foo", "bar", "lorem", "ipsum"))
          .await(0, timeout)
      } tag "not_scala3" // As NamedDatase cannot be ported for now
    }

    "be injected" >> {
      "as default instance" in {
        System.setProperty("config.resource", "test1.conf")

        running(configure _) { app =>
          app.injector
            .instanceOf[InjectDefault]
            .database
            .aka("DB name") must ===("test1").await(1, timeout)
        }
      }

      section("not_scala3")

      "as instance named" >> {
        "'default'" in {
          System.setProperty("config.resource", "test1.conf")

          running(configure _) { app =>
            app.injector
              .instanceOf[InjectDefaultNamed]
              .database
              .aka("DB name") must ===("test1").await(1, timeout)
          }
        } // As NamedDatase cannot be ported for now

        "'foo'" in {
          System.setProperty("config.resource", "test2.conf")

          running() {
            _.injector
              .instanceOf[InjectFooNamed]
              .database
              .aka("DB name") must ===("test2").await(1, timeout)
          }
        }
      }

      "as multiple default and named instance" in {
        System.setProperty("config.resource", "test3.conf")

        running(configure _) {
          _.injector
            .instanceOf[InjectMultiple]
            .databases
            .aka("DB names") must ===(
            ("foo", "foo", "bar", "lorem", "ipsum")
          ).await(1, timeout)
        }
      }

      section("not_scala3") // end
    }

    "be initialized from custom application context" >> {
      def reactiveMongoApi(n: String = "default") = {
        val apiFromCustomCtx =
          new ReactiveMongoApiFromContext(PlayUtil.context, n) {
            lazy val router = play.api.routing.Router.empty

            override lazy val httpFilters =
              Seq.empty[play.api.mvc.EssentialFilter]
          }

        apiFromCustomCtx.reactiveMongoApi
      }

      "successfully with default non-strict URI" in {
        System.setProperty("config.resource", "test1.conf")

        reactiveMongoApi().database.map(_.name).aka("DB resolution") must ===(
          "test1"
        ).await(0, timeout)
      }

      "successfully with other non-strict URI" in {
        System.setProperty("config.resource", "test2.conf")

        reactiveMongoApi("foo").database.map(_.name).aka("DB name") must ===(
          "test2"
        ).await(0, timeout)
      }

      "successfully from composite configuration" in {
        System.setProperty("config.resource", "test3.conf")

        (for {
          _1 <- reactiveMongoApi().database.map(_.name)
          _2 <- reactiveMongoApi("default").database.map(_.name)
          _3 <- reactiveMongoApi("bar").database.map(_.name)
          _4 <- reactiveMongoApi("lorem").database.map(_.name)
          _5 <- reactiveMongoApi("ipsum").database.map(_.name)
        } yield (_1, _2, _3, _4, _5)).aka("DB names") must ===(
          ("foo", "foo", "bar", "lorem", "ipsum")
        ).await(0, timeout)
      }

      "successfully with non-strict URI with prefix" in {
        System.setProperty("config.resource", "test4.conf")

        reactiveMongoApi("default").database
          .map(_.name)
          .aka("DB name") must ===("test4").await(0, timeout)
      }

      "successfully with strict URI" in {
        System.setProperty("config.resource", "strict1.conf")
        reactiveMongoApi().database.map(_.name).aka("DB name") must ===(
          "strict1"
        ).await(0, timeout)
      }

      "and failed with strict URI and unsupported option" in {
        System.setProperty("config.resource", "strict2.conf")
        reactiveMongoApi().database
          .map(_ => {})
          .aka("DB resolution") must throwA[IllegalArgumentException](
          "The connection URI contains unsupported options: foo"
        )

      }
    }
  }

  "Collection" should {
    import reactivemongo.api.bson.collection.{
      BSONCollection,
      BSONCollectionProducer
    }

    "be mixed" in {
      class Foo(val collectionName: String)
          extends WithCollection[BSONCollection] {
        def database = Common.connection.database(Common.db.name)
      }

      new Foo("coll1").collection must beLike[BSONCollection] {
        case _ => ok
      }.await
    }

    "be resolved" in {
      object Foo extends CollectionResolution[BSONCollection]("coll2") {
        def database = Common.connection.database(Common.db.name)
      }

      Foo.collection must beLike[BSONCollection] { case _ => ok }.await
    }
  }

  // ---

  def configuredAppBuilder = {
    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = {
      def toList[T](l: java.util.List[T]): List[T] = {
        val buf = List.newBuilder[T]
        val it = l.iterator()

        while (it.hasNext) {
          buf += it.next()
        }

        buf.result()
      }

      PlayUtil
        .stringList(config, "play.modules.enabled")
        .fold(
          List.empty[String]
        )(toList)
    }

    new GuiceApplicationBuilder()
      .configure(
        "play.modules.enabled" -> (modules :+
          "play.modules.reactivemongo.ReactiveMongoModule")
      )
      .build()
  }
}

import javax.inject.Inject

class InjectDefault @Inject() (api: ReactiveMongoApi) {

  def database(
      implicit
      ec: ExecutionContext
    ): Future[String] =
    api.database.map(_.name)
}

class InjectDefaultNamed @Inject() (
    @NamedDatabase("default") api: ReactiveMongoApi) {

  def database(
      implicit
      ec: ExecutionContext
    ): Future[String] =
    api.database.map(_.name)
}

class InjectFooNamed @Inject() (
    @NamedDatabase("foo") api: ReactiveMongoApi) {

  def database(
      implicit
      ec: ExecutionContext
    ): Future[String] =
    api.database.map(_.name)
}

class InjectMultiple @Inject() (
    defaultApi: ReactiveMongoApi,
    @NamedDatabase("default") namedDefault: ReactiveMongoApi,
    @NamedDatabase("bar") bar: ReactiveMongoApi,
    @NamedDatabase("lorem") lorem: ReactiveMongoApi,
    @NamedDatabase("ipsum") ipsum: ReactiveMongoApi) {

  def databases(
      implicit
      ec: ExecutionContext
    ): Future[(String, String, String, String, String)] = for {
    a <- defaultApi.database.map(_.name)
    b <- namedDefault.database.map(_.name)
    c <- bar.database.map(_.name)
    d <- lorem.database.map(_.name)
    e <- ipsum.database.map(_.name)
  } yield (a, b, c, d, e)
}
