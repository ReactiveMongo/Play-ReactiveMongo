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
import reactivemongo.core.commands._
import scala.concurrent.ExecutionContext

class ReactiveMongoPlugin(app :Application) extends Plugin {
  private var helper :Option[ReactiveMongoHelper] = None

  def db: DefaultDB = helper.get.db
  def dbName: String = helper.get.dbName
  def connection: MongoConnection = helper.get.connection
  def collection(name :String): DefaultCollection = helper.get.db(name)

  override def onStart {
    Logger info "ReactiveMongoPlugin starting..."
    helper = {
      val conf = ReactiveMongoPlugin.parseConf(app)
      try {
        Some(ReactiveMongoHelper(conf._1, conf._2))
      } catch {
        case e :Throwable => {
          throw new PlayException("ReactiveMongoPlugin Initialization Error", "An exception occurred while initializing the ReactiveMongoPlugin.", e)
        }
      }
    }
    helper.map { h =>
      Logger.info("ReactiveMongoPlugin successfully started with db '%s'! Servers:\n\t\t%s"
        .format(
          h.dbName,
          h.servers.map { s => "[%s]".format(s) }.mkString("\n\t\t")
        )
      )
    }
  }

  override def onStop {
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    Logger.info("ReactiveMongoPlugin stops, closing connections...")
    helper.map { h =>
      connection.askClose()(10 seconds).onComplete {
        case e => {
          Logger.info("ReactiveMongo stopped. [" + e + "]")
        }
      }
    }
    helper = None
  }
}

/**
 * MongoDB access methods.
 */
object ReactiveMongoPlugin {
  val DEFAULT_HOST = "localhost:27017"

  def connection(implicit app :Application) = current.connection
  def db(implicit app :Application) = current.db
  def collection(name :String)(implicit app :Application) = current.collection(name)
  def dbName(implicit app :Application) = current.dbName

  /**
    * returns the current instance of the plugin.
    */
  def current(implicit app :Application): ReactiveMongoPlugin = app.plugin[ReactiveMongoPlugin] match {
    case Some(plugin) => plugin
    case _ => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  /**
   * returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java).
   */
  def current(app :play.Application): ReactiveMongoPlugin = app.plugin(classOf[ReactiveMongoPlugin]) match {
    case plugin if plugin != null => plugin
    case _ => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  private def parseConf(app :Application): (String, List[String]) = {
    (
      app.configuration.getString("mongodb.db") match {
        case Some(db) => db
        case _ => throw app.configuration.globalError("Missing configuration key 'mongodb.db'!")
      },
      app.configuration.getStringList("mongodb.servers") match {
        case Some(list) => scala.collection.JavaConversions.collectionAsScalaIterable(list).toList
        case None => List(DEFAULT_HOST) //throw app.configuration.globalError("Missing configuration key 'mongodb.servers' (should be a list of servers)!")
      }
    )
  }
}

private[reactivemongo] case class ReactiveMongoHelper(dbName: String, servers: List[String]) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val connection = MongoConnection(servers)
  lazy val db = DB(dbName, connection)

  def collection(name :String): Collection = db(name)
}
