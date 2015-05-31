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

import scala.concurrent.{ Await, ExecutionContext }
import scala.util.control.NonFatal

/**
 * Deprecated since Play Framework 2.4 release. Plugins should be modules
 */
@deprecated("Use ReactiveMongoModule and ReactiveMongoApi.", since = "0.12.1")
class ReactiveMongoPlugin(app: Application) extends Plugin {
  private var _helper: Option[ReactiveMongoHelper] = None
  def helper = _helper.getOrElse(throw new ReactiveMongoPluginException("ReactiveMongoPlugin error: no ReactiveMongoHelper available?"))

  override def onStart() {
    Logger info "ReactiveMongoPlugin starting..."
    try {
      val conf = DefaultReactiveMongoApi.parseConf(app.configuration)
      _helper = Some(ReactiveMongoHelper(conf, app))
      Logger.info("ReactiveMongoPlugin successfully started with db '%s'! Servers:\n\t\t%s"
        .format(
          conf.db.get,
          conf.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")))
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
        case e => {
          Logger.info("ReactiveMongo Connections stopped. [" + e + "]")
        }
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
@deprecated("Use ReactiveMongoModule and ReactiveMongoApi.", since = "0.12.1")
object ReactiveMongoPlugin {
  /** Returns the current instance of the driver. */
  def driver(implicit app: Application) = current.helper.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection(implicit app: Application) = current.helper.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db(implicit app: Application) = current.helper.db

  /** Returns the current instance of the plugin. */
  def current(implicit app: Application): ReactiveMongoPlugin = app.plugin[ReactiveMongoPlugin] match {
    case Some(plugin) => plugin
    case _            => throw new ReactiveMongoPluginException("The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  /** Returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java). */
  def current(app: play.Application): ReactiveMongoPlugin = app.plugin(classOf[ReactiveMongoPlugin]) match {
    case plugin if plugin != null => plugin
    case _                        => throw new ReactiveMongoPluginException("The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }
}

private[reactivemongo] case class ReactiveMongoHelper(parsedURI: MongoConnection.ParsedURI, app: Application) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val driver = new MongoDriver(Akka.system(app))
  lazy val connection = driver.connection(parsedURI)
  lazy val db = DB(parsedURI.db.get, connection)
}
