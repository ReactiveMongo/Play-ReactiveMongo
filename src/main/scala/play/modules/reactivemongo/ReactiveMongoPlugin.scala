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
import play.api.libs.concurrent.Akka
import reactivemongo.api._
import reactivemongo.core.commands._
import reactivemongo.core.nodeset.Authenticate
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success }

class ReactiveMongoPlugin(app: Application) extends Plugin {
  private var _helper: Option[ReactiveMongoHelper] = None
  def helper = _helper.getOrElse(throw new RuntimeException("ReactiveMongoPlugin error: no ReactiveMongoHelper available?"))

  override def onStart {
    Logger info "ReactiveMongoPlugin starting..."
    _helper = {
      val conf = ReactiveMongoPlugin.parseConf(app)
      try {
        Some(ReactiveMongoHelper(conf._1, conf._2, conf._3, conf._4, app))
      } catch {
        case e: Throwable => {
          throw new PlayException("ReactiveMongoPlugin Initialization Error", "An exception occurred while initializing the ReactiveMongoPlugin.", e)
        }
      }
    }
    _helper.map { h =>
      Logger.info("ReactiveMongoPlugin successfully started with db '%s'! Servers:\n\t\t%s"
        .format(
          h.dbName,
          h.servers.map { s => "[%s]".format(s) }.mkString("\n\t\t")))
    }
  }

  override def onStop {
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    Logger.info("ReactiveMongoPlugin stops, closing connections...")
    _helper.map { h =>
      val f = h.connection.askClose()(10 seconds)
      f.onComplete {
        case e => {
          Logger.info("ReactiveMongo Connections stopped. [" + e + "]")
        }
      }
      Await.ready(f, 10 seconds)
      h.driver.close()
    }
    _helper = None
  }
}

/**
 * MongoDB access methods.
 */
object ReactiveMongoPlugin {
  val DEFAULT_HOST = "localhost:27017"

  import play.modules.reactivemongo.json.collection._

  /** Returns the current instance of the driver. */
  def driver(implicit app: Application) = current.helper.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection(implicit app: Application) = current.helper.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db(implicit app: Application) = current.helper.db

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

  private def parseConf(app: Application): (String, List[String], List[Authenticate], Option[Int]) = {
    val (dbName, servers, auth) = app.configuration.getString("mongodb.uri") match {
      case Some(uri) =>
        MongoConnection.parseURI(uri) match {
          case Success(MongoConnection.ParsedURI(hosts, Some(db), auth)) =>
            (db, hosts.map(h => h._1 + ":" + h._2), auth.toList)
          case Success(MongoConnection.ParsedURI(_, None, _)) =>
            throw app.configuration.globalError(s"Missing database name in mongodb.uri '$uri'")
          case Failure(e) => throw app.configuration.globalError(s"Invalid mongodb.uri '$uri'", Some(e))
        }
      case _ =>
        (
          app.configuration.getString("mongodb.db") match {
            case Some(db) => db
            case _        => throw app.configuration.globalError("Missing configuration key 'mongodb.db'!")
          },
          app.configuration.getStringList("mongodb.servers") match {
            case Some(list) => scala.collection.JavaConversions.collectionAsScalaIterable(list).toList
            case None       => List(DEFAULT_HOST)
          },
          List())
    }
    val nbChannelsPerNode = app.configuration.getInt("mongodb.channels")
    (dbName, servers, auth, nbChannelsPerNode)
  }
}

private[reactivemongo] case class ReactiveMongoHelper(dbName: String, servers: List[String], auth: List[Authenticate], nbChannelsPerNode: Option[Int], app: Application) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val driver = new MongoDriver(Akka.system(app))
  lazy val connection = nbChannelsPerNode match {
    case Some(numberOfChannels) => driver.connection(servers, auth, nbChannelsPerNode = numberOfChannels)
    case _                      => driver.connection(servers, auth)
  }
  lazy val db = DB(dbName, connection)
}
