package play.modules.reactivemongo

import scala.util.Failure

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }

import reactivemongo.api.{ AsyncDriver, DB, MongoConnection }

import reactivemongo.api.bson.collection.BSONSerializationPack

import reactivemongo.api.gridfs.GridFS

/**
 * Default implementation of ReactiveMongoApi.
 *
 * @param dbName the name of the database
 */
final class DefaultReactiveMongoApi(
    parsedUri: MongoConnection.ParsedURIWithDB,
    dbName: String,
    strictMode: Boolean,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle)(
    implicit
    ec: ExecutionContext) extends ReactiveMongoApi {

  import DefaultReactiveMongoApi._

  private lazy val resourceTimeout: FiniteDuration =
    10.seconds // TODO: Configuration

  lazy val asyncDriver = AsyncDriver(configuration.underlying)

  lazy val connection = {
    val con = Await.result(asyncDriver.connect(parsedUri, name = Some(dbName), strictMode), resourceTimeout)

    registerDriverShutdownHook(con, asyncDriver)

    con
  }

  def database: Future[DB] = {
    logger.debug(s"Resolving database '$dbName' ... ($parsedUri)")

    connection.database(dbName)
  }

  def asyncGridFS: Future[GridFS[BSONSerializationPack.type]] =
    database.map(_.gridfs("fs"))

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: AsyncDriver): Unit = applicationLifecycle.addStopHook { () =>
    logger.info("ReactiveMongoApi stopping...")

    def closeDriver() = Await.result(
      mongoDriver.close(resourceTimeout), resourceTimeout)

    Await.ready(connection.close()(resourceTimeout).map { _ =>
      logger.info("ReactiveMongoApi connections are stopped")
    }.andThen {
      case Failure(reason) =>
        reason.printStackTrace()
        closeDriver() // Close anyway

      case _ => closeDriver()
    }, 12.seconds)
  }
}

private[reactivemongo] object DefaultReactiveMongoApi {
  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"

  private[reactivemongo] val logger = Logger(this.getClass)

  private def parseURI(key: String, uri: String)(implicit ec: ExecutionContext): Option[MongoConnection.ParsedURIWithDB] = scala.util.Try(Await.result(MongoConnection.fromStringWithDB(uri), 10.seconds)).recoverWith {
    case cause =>
      logger.warn(s"Invalid connection URI '$key': $uri", cause)
      Failure[MongoConnection.ParsedURIWithDB](cause)
  }.toOption

  private[reactivemongo] case class BindingInfo(
      strict: Boolean,
      database: String,
      uri: MongoConnection.ParsedURIWithDB)

  private[reactivemongo] def parseConfiguration(configuration: Configuration)(implicit ec: ExecutionContext): Seq[(String, BindingInfo)] = Config.configuration(configuration)(
    "mongodb") match {
      case Some(subConf) => {
        val parsed = Seq.newBuilder[(String, BindingInfo)]
        val str = Config.string(subConf) _

        str("uri").map("mongodb.uri" -> _).orElse(str("default.uri").
          map("mongodb.default.uri" -> _)).flatMap {
          case (key, uri) => parseURI(key, uri).map { u =>
            import u.db
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
          case (key, input) => parseURI(key, input).foreach { u =>
            import u.db
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
