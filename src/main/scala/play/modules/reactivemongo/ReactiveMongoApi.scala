package play.modules.reactivemongo

import javax.inject.Inject

import scala.util.{ Failure, Success }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import akka.actor.ActorSystem

import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }

import reactivemongo.api.{
  DefaultDB,
  DB,
  MongoConnection,
  MongoConnectionOptions,
  MongoDriver
}
import reactivemongo.api.gridfs.GridFS
import reactivemongo.core.nodeset.Authenticate

import play.modules.reactivemongo.json.JSONSerializationPack

/**
 * MongoDB API
 */
trait ReactiveMongoApi {
  def driver: MongoDriver
  def connection: MongoConnection
  def db: DefaultDB
  def gridFS: GridFS[JSONSerializationPack.type]
}

/**
 * Default implementation of ReactiveMongoApi.
 */
final class DefaultReactiveMongoApi @Inject() (
    actorSystem: ActorSystem,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle) extends ReactiveMongoApi {

  import DefaultReactiveMongoApi._

  override lazy val driver = new MongoDriver(Some(configuration.underlying))
  override lazy val connection = driver.connection(parsedUri)

  override lazy val db: DefaultDB = {
    Logger.info("ReactiveMongoApi starting...")

    parsedUri.db.fold[DefaultDB](throw configuration.globalError(
      s"cannot resolve database from URI: $parsedUri")) { dbUri =>

      val db = DB(dbUri, connection)

      registerDriverShutdownHook(connection, driver)

      Logger.info(s"""ReactiveMongoApi successfully started with DB '$dbUri'! Servers:\n\t\t${parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")}""")

      db
    }
  }

  def gridFS = {
    import play.modules.reactivemongo.json.collection._
    GridFS[JSONSerializationPack.type](db)
  }

  private lazy val parsedUri = parseConf(configuration)

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit = applicationLifecycle.addStopHook { () =>
    Future {
      Logger.info("ReactiveMongoApi stopping...")
      val f = connection.askClose()(10.seconds)

      f.onComplete {
        case e => Logger.info(s"ReactiveMongoApi connections stopped. [$e]")
      }

      Await.ready(f, 10.seconds)
      mongoDriver.close()
    }
  }
}

private[reactivemongo] object DefaultReactiveMongoApi {
  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"

  private def parseLegacy(configuration: Configuration): MongoConnection.ParsedURI = {
    val db = configuration.getString("mongodb.db").getOrElse(throw configuration.globalError("Missing configuration key 'mongodb.db'!"))
    val uris = configuration.getStringList("mongodb.servers") match {
      case Some(list) => scala.collection.JavaConversions.collectionAsScalaIterable(list).toList
      case None       => List(DefaultHost)
    }

    val nodes = uris.map { uri =>
      uri.split(':').toList match {
        case host :: port :: Nil => host -> {
          try {
            val p = port.toInt
            if (p > 0 && p < 65536) p
            else throw configuration.globalError(s"Could not parse URI '$uri': invalid port '$port'")
          } catch {
            case _: NumberFormatException => throw configuration.globalError(s"Could not parse URI '$uri': invalid port '$port'")
          }
        }
        case host :: Nil => host -> DefaultPort
        case _           => throw configuration.globalError(s"Could not parse host '$uri'")
      }
    }

    var opts = MongoConnectionOptions()

    for (nbChannelsPerNode <- configuration.getInt("mongodb.options.nbChannelsPerNode"))
      opts = opts.copy(nbChannelsPerNode = nbChannelsPerNode)
    for (authSource <- configuration.getString("mongodb.options.authSource"))
      opts = opts.copy(authSource = Some(authSource))
    for (connectTimeoutMS <- configuration.getInt("mongodb.options.connectTimeoutMS"))
      opts = opts.copy(connectTimeoutMS = connectTimeoutMS)
    for (tcpNoDelay <- configuration.getBoolean("mongodb.options.tcpNoDelay"))
      opts = opts.copy(tcpNoDelay = tcpNoDelay)
    for (keepAlive <- configuration.getBoolean("mongodb.options.keepAlive"))
      opts = opts.copy(keepAlive = keepAlive)

    val authenticate = {
      val username = configuration.getString("mongodb.credentials.username")
      val password = configuration.getString("mongodb.credentials.password")

      if (username.isDefined && password.isEmpty || username.isEmpty && password.isDefined)
        throw configuration.globalError("Could not parse credentials: missing username or password")
      else if (username.isDefined && password.isDefined)
        Some(Authenticate.apply(opts.authSource.getOrElse(db), username.get, password.get))
      else None
    }

    MongoConnection.ParsedURI(
      hosts = nodes,
      options = opts,
      ignoredOptions = Nil,
      db = Some(db),
      authenticate = authenticate)
  }

  def parseConf(configuration: Configuration): MongoConnection.ParsedURI =
    configuration.getString("mongodb.uri") match {
      case Some(uri) => MongoConnection.parseURI(uri) match {
        case Success(parsedURI) if parsedURI.db.isDefined =>
          parsedURI
        case Success(_) =>
          throw configuration.globalError(s"Missing database name in mongodb.uri '$uri'")
        case Failure(e) => throw configuration.globalError(s"Invalid mongodb.uri '$uri'", Some(e))
      }

      case _ => parseLegacy(configuration)
    }
}
