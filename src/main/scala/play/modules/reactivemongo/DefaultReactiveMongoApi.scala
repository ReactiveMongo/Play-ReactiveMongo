package play.modules.reactivemongo

import scala.util.{ Failure, Success }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import akka.actor.ActorSystem

import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }

import reactivemongo.api.{
  DefaultDB,
  MongoConnection,
  MongoConnectionOptions,
  MongoDriver,
  ReadPreference,
  ScramSha1Authentication
}
import reactivemongo.api.commands.WriteConcern
import reactivemongo.api.gridfs.GridFS
import reactivemongo.core.nodeset.Authenticate

import reactivemongo.play.json.JSONSerializationPack

/**
 * Default implementation of ReactiveMongoApi.
 */
final class DefaultReactiveMongoApi(
    name: String,
    parsedUri: MongoConnection.ParsedURI,
    dbName: String,
    strictMode: Boolean,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
) extends ReactiveMongoApi {
  import reactivemongo.play.json.collection._
  import DefaultReactiveMongoApi._

  @deprecated("Use `new DefaultReactiveMongoApi(configuration, applicationLifecycle)`", "0.12.0")
  def this(
    parsedUri: MongoConnection.ParsedURI,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
  ) = this("default", parsedUri, parsedUri.db.get, false,
    configuration, applicationLifecycle)

  @deprecated("Use `new DefaultReactiveMongoApi(configuration, applicationLifecycle)`", "0.12.0")
  def this(
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
  ) = this(
    DefaultReactiveMongoApi.parseConf(configuration),
    configuration, applicationLifecycle
  )

  @deprecated("Use `new DefaultReactiveMongoApi(configuration, applicationLifecycle)`", "0.12.0")
  def this(
    actorSystem: ActorSystem,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
  ) = this(
    DefaultReactiveMongoApi.parseConf(configuration),
    configuration, applicationLifecycle
  )

  lazy val driver = new MongoDriver(Some(configuration.underlying))
  lazy val connection = {
    val con = driver.connection(parsedUri, strictMode).get
    registerDriverShutdownHook(con, driver)
    con
  }

  @deprecated("Use `DefaultReactiveMongoApi.database`", "0.12.0")
  lazy val db: DefaultDB = {
    import scala.concurrent.ExecutionContext.Implicits.global

    logger.debug(s"Resolving database '$dbName' ...")

    connection(dbName)
  }

  def database: Future[DefaultDB] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    logger.debug(s"Resolving database '$dbName' ... ($parsedUri)")

    connection.database(dbName)
  }

  @deprecated("Use `DefaultReactiveMongoApi.asyncGridFS`", "0.12.0")
  def gridFS = GridFS[JSONSerializationPack.type](db)

  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    database.map(GridFS[JSONSerializationPack.type](_))
  }

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    applicationLifecycle.addStopHook { () =>
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
}

private[reactivemongo] object DefaultReactiveMongoApi {
  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"

  private[reactivemongo] val logger = Logger(this.getClass)

  private def parseLegacy(configuration: Configuration): MongoConnection.ParsedURI = {
    val db = configuration.getString("mongodb.db").getOrElse(
      throw configuration.globalError(
        "Missing configuration key 'mongodb.db'!"
      )
    )

    val uris = configuration.getStringList("mongodb.servers") match {
      case Some(list) => scala.collection.JavaConversions.
        collectionAsScalaIterable(list).toList

      case _ => List(DefaultHost)
    }

    val nodes = uris.map { uri =>
      uri.split(':').toList match {
        case host :: port :: Nil => host -> {
          try {
            val p = port.toInt
            if (p > 0 && p < 65536) p
            else throw configuration.globalError(
              s"Could not parse URI '$uri': invalid port '$port'"
            )
          } catch {
            case _: NumberFormatException => throw configuration.globalError(
              s"Could not parse URI '$uri': invalid port '$port'"
            )
          }
        }
        case host :: Nil => host -> DefaultPort
        case _ => throw configuration.globalError(
          s"Could not parse host '$uri'"
        )
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
        wtimeout = Some(ms)
      ))
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
      authenticate = authenticate
    )
  }

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

  private val defaultKeys = Set("uri", "default",
    /* TODO: remove following: */ "servers", "credentials", "options")

  private[reactivemongo] case class BindingInfo(
    strict: Boolean,
    database: String,
    uri: MongoConnection.ParsedURI
  )

  private[reactivemongo] def parseConfiguration(configuration: Configuration): Seq[(String, BindingInfo)] = configuration.getConfig("mongodb") match {
    case Some(subConf) => {
      val parsed = Seq.newBuilder[(String, BindingInfo)]

      subConf.getString("uri").map("mongodb.uri" -> _).
        orElse(subConf.getString("default.uri").
          map("mongodb.default.uri" -> _)).flatMap {
          case (key, uri) => parseURI(key, uri).map {
            case (u, db) =>
              val strictKey = s"${key.dropRight(4)}.connection.strictUri"
              "default" -> BindingInfo(
                strict = configuration.getBoolean(strictKey).getOrElse(false),
                database = db,
                uri = u
              )
          }
        }.foreach { parsed += _ }

      val other = subConf.entrySet.iterator.collect {
        case (key, value) if (
          key.endsWith(".uri") && value.unwrapped.isInstanceOf[String]
        ) => s"mongodb.$key" -> value.unwrapped.asInstanceOf[String]
      }

      other.foreach {
        case (key, input) => parseURI(key, input).foreach {
          case (u, db) =>
            val baseKey = key.dropRight(4)
            val strictKey = s"$baseKey.connection.strictUri"
            val name = baseKey.drop(8)

            parsed += name -> BindingInfo(
              strict = configuration.getBoolean(strictKey).getOrElse(false),
              database = db,
              uri = u
            )
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

  @deprecated("Use `DefaultReactiveMongoApi.parseConfiguration`", "0.12.0")
  def parseConf(configuration: Configuration): MongoConnection.ParsedURI =
    configuration.getString("mongodb.uri") match {
      case Some(uri) => MongoConnection.parseURI(uri) match {
        case Success(parsedURI) if parsedURI.db.isDefined =>
          parsedURI

        case Success(_) => throw configuration.globalError(
          s"Missing database name in mongodb.uri '$uri'"
        )

        case Failure(e) => throw configuration.globalError(
          s"Invalid mongodb.uri '$uri'", Some(e)
        )
      }

      case _ => parseLegacy(configuration)
    }
}
