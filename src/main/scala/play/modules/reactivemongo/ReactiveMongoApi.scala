package play.modules.reactivemongo

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }
import reactivemongo.api._
import reactivemongo.core.nodeset.Authenticate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

/**
 * MongoDB API
 */
trait ReactiveMongoApi {
  def driver: MongoDriver
  def connection: MongoConnection
  def db: DB
}

/**
 * Default implementation of ReactiveMongoApi.
 */
final class DefaultReactiveMongoApi @Inject() (actorSystem: ActorSystem,
                                               configuration: Configuration,
                                               applicationLifecycle: ApplicationLifecycle) extends ReactiveMongoApi {
  import DefaultReactiveMongoApi._

  override lazy val driver = new MongoDriver(actorSystem)
  override lazy val connection = driver.connection(parsedUri)

  override lazy val db: DB = {
    Logger.info("DefaultReactiveMongoApi starting...")
    val db = DB(parsedUri.db.get, connection)
    registerDriverShutdownHook(connection, driver)
    Logger.info("DefaultReactiveMongoApi successfully started with db '%s'! Servers:\n\t\t%s"
      .format(
        parsedUri.db.get,
        parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")))
    db
  }

  private lazy val parsedUri = parseConf(configuration)

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit = {
    applicationLifecycle.addStopHook { () =>
      Future {
        Logger.info("DefaultReactiveMongoApi stopping...")
        val f = connection.askClose()(10.seconds)
        f.onComplete {
          case e => {
            Logger.info("DefaultReactiveMongoApi connections stopped. [" + e + "]")
          }
        }
        Await.ready(f, 10.seconds)
        mongoDriver.close()
      }
    }
  }

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
            if (p > 0 && p < 65536)
              p
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

  private def parseConf(configuration: Configuration): MongoConnection.ParsedURI = {
    configuration.getString("mongodb.uri") match {
      case Some(uri) =>
        MongoConnection.parseURI(uri) match {
          case Success(parsedURI) if parsedURI.db.isDefined =>
            parsedURI
          case Success(_) =>
            throw configuration.globalError(s"Missing database name in mongodb.uri '$uri'")
          case Failure(e) => throw configuration.globalError(s"Invalid mongodb.uri '$uri'", Some(e))
        }
      case _ =>
        parseLegacy(configuration)
    }
  }
}

object DefaultReactiveMongoApi {
  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"
}
