package play.modules.reactivemongo

import javax.inject.Inject

import scala.util.{ Failure, Success }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import akka.actor.ActorSystem

import play.api.inject.ApplicationLifecycle
import play.api.{
  ApplicationLoader,
  BuiltInComponentsFromContext,
  Configuration,
  Logger
}

import reactivemongo.api.{
  DefaultDB,
  DB,
  MongoConnection,
  MongoConnectionOptions,
  MongoDriver,
  ReadPreference,
  ScramSha1Authentication
}
import reactivemongo.api.commands.WriteConcern
import reactivemongo.api.gridfs.GridFS
import reactivemongo.core.nodeset.Authenticate

import reactivemongo.play.json._, collection._

/**
 * MongoDB API
 */
trait ReactiveMongoApi {
  def driver: MongoDriver
  def connection: MongoConnection
  def database: Future[DefaultDB]
  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]]

  // TODO: Remove

  /** See [[database]] */
  def db: DefaultDB

  /** See [[asyncGridFS]] */
  def gridFS: GridFS[JSONSerializationPack.type]
}

trait ReactiveMongoApiComponents {
  /** The configuration */
  def configuration: Configuration

  /** The application lifecycle */
  def applicationLifecycle: ApplicationLifecycle

  /** The API initialized according the current configuration */
  lazy val reactiveMongoApi: ReactiveMongoApi =
    new DefaultReactiveMongoApi(configuration, applicationLifecycle)
}

/**
 * Can be used for a custom application loader.
 *
 * {{{
 * import play.api.ApplicationLoader
 *
 * class MyApplicationLoader extends ApplicationLoader {
 *   def load(context: ApplicationLoader.Context) =
 *     new MyComponents(context).application
 * }
 *
 * class MyComponents(context: ApplicationLoader.Context)
 *     extends ReactiveMongoApiFromContext(context) {
 *   lazy val router = play.api.routing.Router.empty
 * }
 * }}}
 */
abstract class ReactiveMongoApiFromContext(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with ReactiveMongoApiComponents {

}

/**
 * Default implementation of ReactiveMongoApi.
 */
final class DefaultReactiveMongoApi @Inject() (
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle) extends ReactiveMongoApi {

  @deprecated("Use `new DefaultReactiveMongoApi(configuration, applicationLifecycle)`", "0.12.0")
  def this(actorSystem: ActorSystem,
           configuration: Configuration,
           applicationLifecycle: ApplicationLifecycle) =
    this(configuration, applicationLifecycle)

  import DefaultReactiveMongoApi._

  lazy val driver = new MongoDriver(Some(configuration.underlying))
  lazy val connection = {
    val con = driver.connection(parsedUri)
    registerDriverShutdownHook(con, driver)
    con
  }

  private lazy val dbName: String = parsedUri.db.fold[String](
    throw configuration.globalError(
      s"cannot resolve the database name from URI: $parsedUri")) { name =>
      Logger.info(s"""ReactiveMongoApi successfully configured with DB '$name'! Servers:\n\t\t${parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")}""")
      name
    }

  lazy val db: DefaultDB = {
    import scala.concurrent.ExecutionContext.Implicits.global

    Logger.info("ReactiveMongoApi starting...")

    connection(dbName)
  }

  def database: Future[DefaultDB] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    Logger.info("ReactiveMongoApi starting...")

    connection.database(dbName)
  }

  def gridFS = GridFS[JSONSerializationPack.type](db)

  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    database.map(GridFS[JSONSerializationPack.type](_))
  }

  private lazy val parsedUri = parseConf(configuration)

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    applicationLifecycle.addStopHook { () =>
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

    configuration.getInt("mongodb.options.nbChannelsPerNode").
      foreach { nb => opts = opts.copy(nbChannelsPerNode = nb) }

    configuration.getString("mongodb.options.authSource").
      foreach { src => opts = opts.copy(authSource = Some(src)) }

    configuration.getInt("mongodb.options.connectTimeoutMS").
      foreach { ms => opts = opts.copy(connectTimeoutMS = ms) }

    configuration.getBoolean("mongodb.options.tcpNoDelay").
      foreach { delay => opts = opts.copy(tcpNoDelay = delay) }

    configuration.getBoolean("mongodb.options.keepAlive").
      foreach { keepAlive => opts = opts.copy(keepAlive = keepAlive) }

    configuration.getBoolean("mongodb.options.ssl.enabled").
      foreach { ssl => opts = opts.copy(sslEnabled = ssl) }

    configuration.getBoolean("mongodb.options.ssl.allowsInvalidCert").
      foreach { allows => opts = opts.copy(sslAllowsInvalidCert = allows) }

    configuration.getString("mongodb.options.authMode").foreach {
      case "scram-sha1" =>
        opts = opts.copy(authMode = ScramSha1Authentication)

      case _ => ()
    }

    configuration.getString("mongodb.options.writeConcern").foreach {
      case "unacknowledged" =>
        opts = opts.copy(writeConcern = WriteConcern.Unacknowledged)

      case "acknowledged" =>
        opts = opts.copy(writeConcern = WriteConcern.Acknowledged)

      case "journaled" =>
        opts = opts.copy(writeConcern = WriteConcern.Journaled)

      case "default" =>
        opts = opts.copy(writeConcern = WriteConcern.Default)

      case _ => ()
    }

    val IntRe = "^([0-9]+)$".r

    configuration.getString("mongodb.options.writeConcernW").foreach {
      case "majority" => opts = opts.copy(writeConcern = opts.writeConcern.
        copy(w = WriteConcern.Majority))

      case IntRe(str) => opts = opts.copy(writeConcern = opts.writeConcern.
        copy(w = WriteConcern.WaitForAknowledgments(str.toInt)))

      case tag => opts = opts.copy(writeConcern = opts.writeConcern.
        copy(w = WriteConcern.TagSet(tag)))

    }

    configuration.getBoolean("mongodb.options.writeConcernJ").foreach { jed =>
      opts = opts.copy(writeConcern = opts.writeConcern.copy(j = jed))
    }

    configuration.getInt("mongodb.options.writeConcernTimeout").foreach { ms =>
      opts = opts.copy(writeConcern = opts.writeConcern.copy(
        wtimeout = Some(ms)))
    }

    configuration.getString("mongodb.options.readPreference").foreach {
      case "primary" =>
        opts = opts.copy(readPreference = ReadPreference.primary)

      case "primaryPreferred" =>
        opts = opts.copy(readPreference = ReadPreference.primaryPreferred)

      case "secondary" =>
        opts = opts.copy(readPreference = ReadPreference.secondary)

      case "secondaryPreferred" =>
        opts = opts.copy(readPreference = ReadPreference.secondaryPreferred)

      case "nearest" =>
        opts = opts.copy(readPreference = ReadPreference.nearest)

      case _ => ()
    }

    val authenticate: Option[Authenticate] = for {
      username <- configuration.getString("mongodb.credentials.username")
      password <- configuration.getString("mongodb.credentials.password")
    } yield Authenticate(opts.authSource.getOrElse(db), username, password)

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
