/*
 * Copyright 2012 Pascal Voitot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.modules.reactivemongo

import play.api._
import reactivemongo.api._
import reactivemongo.api.commands.WriteConcern
import reactivemongo.core.nodeset.Authenticate

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal

class ReactiveMongoPlugin(app: Application) extends Plugin {
  private var _helper: Option[ReactiveMongoHelper] = None

  def helper = _helper.getOrElse(throw new RuntimeException(
    "ReactiveMongoPlugin error: no ReactiveMongoHelper available?"
  ))

  override def onStart() {
    Logger info "ReactiveMongoPlugin starting..."

    try {
      val conf = ReactiveMongoPlugin.parseConf(app.configuration)
      _helper = Some(ReactiveMongoHelper(conf, app))
      Logger.info("ReactiveMongoPlugin successfully started with db '%s'! Servers:\n\t\t%s"
        .format(
          conf.db.get,
          conf.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")
        ))
    } catch {
      case NonFatal(e) =>
        throw new ReactiveMongoPluginException("An exception occurred while initializing the ReactiveMongoPlugin.", e)
    }
  }

  override def onStop() {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    Logger.info("ReactiveMongoPlugin stops, closing connections...")
    _helper.foreach { h =>
      val f = h.connection.askClose()(10.seconds)
      f.onComplete {
        case e => Logger.info(s"ReactiveMongo Connections stopped. [$e]")
      }
      Await.ready(f, 10.seconds)
      h.driver.close()
    }
    _helper = None
  }
}

/**
 * MongoDB access methods.
 */
object ReactiveMongoPlugin {
  import scala.util.{ Failure, Success }

  val DefaultPort = 27017
  val DefaultHost = "localhost:27017"

  /** Returns the current instance of the driver. */
  def driver(implicit app: Application) = current.helper.driver

  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection(implicit app: Application) = current.helper.connection

  /** Returns the default database (as specified in `application.conf`). */
  def db(implicit app: Application) = current.helper.db

  /** Returns the default database (with failover). */
  def database(implicit app: Application) = current.helper.database

  /** Returns the current instance of the plugin. */
  def current(implicit app: Application): ReactiveMongoPlugin = app.plugin[ReactiveMongoPlugin] match {
    case Some(plugin) => plugin
    case _            => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  /** Returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java). */
  def current(app: play.Application): ReactiveMongoPlugin = app.plugin(classOf[ReactiveMongoPlugin]) match {
    case plugin if plugin != null => plugin
    case _                        => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
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

private[reactivemongo] case class ReactiveMongoHelper(parsedURI: MongoConnection.ParsedURI, app: Application) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val driver = new MongoDriver(Option(app.configuration.underlying))
  lazy val connection = {
    val strictMode = app.configuration.
      getBoolean("mongodb.connection.strictUri").getOrElse(false)

    driver.connection(parsedURI, strictMode).get
  }

  lazy val db = DB(parsedURI.db.get, connection)

  def database: Future[DefaultDB] =
    parsedURI.db.fold(Future.failed[DefaultDB](new ReactiveMongoPluginException(
      "missing database name"
    )))(connection.database(_))
}
