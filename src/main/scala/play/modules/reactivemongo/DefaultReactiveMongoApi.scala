package play.modules.reactivemongo

import scala.util.{ Failure, Success }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import reactivemongo.api.gridfs.GridFS

import reactivemongo.play.json.JSONSerializationPack

/**
 * Default implementation of ReactiveMongoApi.
 */
final class DefaultReactiveMongoApi(
    parsedUri: MongoConnection.ParsedURI,
    dbName: String,
    strictMode: Boolean,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle)(
    implicit
    ec: ExecutionContext) extends ReactiveMongoApi {

  @com.github.ghik.silencer.silent
  @deprecated("Use the constructor without the unused `name`", "0.17.0")
  def this(
    name: String,
    parsedUri: MongoConnection.ParsedURI,
    dbName: String,
    strictMode: Boolean,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle) = this(
    parsedUri, dbName, strictMode, configuration, applicationLifecycle)(
    play.api.libs.concurrent.Execution.Implicits.defaultContext)

  import reactivemongo.play.json.collection._
  import DefaultReactiveMongoApi._

  lazy val driver = new MongoDriver(Some(configuration.underlying), None)
  lazy val connection = {
    val con = driver.connection(parsedUri, strictMode).get
    registerDriverShutdownHook(con, driver)
    con
  }

  def database: Future[DefaultDB] = {
    logger.debug(s"Resolving database '$dbName' ... ($parsedUri)")

    connection.database(dbName)
  }

  @deprecated("Use `DefaultReactiveMongoApi.asyncGridFS`", "0.12.0")
  def gridFS = GridFS[JSONSerializationPack.type](
    Await.result(database, 10.seconds))

  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]] =
    database.map(GridFS[JSONSerializationPack.type](_))

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit = applicationLifecycle.addStopHook { () =>
    logger.info("ReactiveMongoApi stopping...")

    Await.ready(connection.askClose()(10.seconds).map { _ =>
      logger.info("ReactiveMongoApi connections are stopped")
    }.andThen {
      case Failure(reason) =>
        reason.printStackTrace()
        mongoDriver.close() // Close anyway

      case _ => mongoDriver.close()
    }, 12.seconds)
  }
}

private[reactivemongo] object DefaultReactiveMongoApi {
  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"

  private[reactivemongo] val logger = Logger(this.getClass)

  private def parseURI(key: String, uri: String): Option[(MongoConnection.ParsedURI, String)] = MongoConnection.parseURI(uri) match {
    case Success(parsedURI) => parsedURI.db match {
      case Some(db) => Some(parsedURI -> db)
      case _ => {
        logger.warn(s"Missing database name in '$key': $uri")
        None
      }
    }

    case Failure(e) => {
      logger.warn(s"Invalid connection URI '$key': $uri", e)
      None
    }
  }

  private[reactivemongo] case class BindingInfo(
      strict: Boolean,
      database: String,
      uri: MongoConnection.ParsedURI)

  private[reactivemongo] def parseConfiguration(configuration: Configuration): Seq[(String, BindingInfo)] = Config.configuration(configuration)(
    "mongodb") match {
      case Some(subConf) => {
        val parsed = Seq.newBuilder[(String, BindingInfo)]
        val str = Config.string(subConf) _

        str("uri").map("mongodb.uri" -> _).orElse(str("default.uri").
          map("mongodb.default.uri" -> _)).flatMap {
          case (key, uri) => parseURI(key, uri).map {
            case (u, db) =>
              val strictKey = s"${key.dropRight(4)}.connection.strictUri"
              "default" -> BindingInfo(
                strict = Config.boolean(configuration)(
                  strictKey).getOrElse(false),
                database = db,
                uri = u)
          }
        }.foreach { parsed += _ }

        val other = subConf.entrySet.iterator.collect {
          case (key, value) if (
            key.endsWith(".uri") && value.unwrapped.isInstanceOf[String]) => s"mongodb.$key" -> value.unwrapped.asInstanceOf[String]
        }

        other.foreach {
          case (key, input) => parseURI(key, input).foreach {
            case (u, db) =>
              val baseKey = key.dropRight(4)
              val strictKey = s"$baseKey.connection.strictUri"
              val name = baseKey.drop(8)

              parsed += name -> BindingInfo(
                strict = Config.boolean(configuration)(
                  strictKey).getOrElse(false),
                database = db,
                uri = u)
          }
        }

        val mongoConfigs = parsed.result()

        if (mongoConfigs.isEmpty) {
          logger.warn("No configuration in the 'mongodb' section")
        }

        mongoConfigs
      }

      case _ => {
        logger.warn("No 'mongodb' section found in the configuration")
        Seq.empty
      }
    }
}
