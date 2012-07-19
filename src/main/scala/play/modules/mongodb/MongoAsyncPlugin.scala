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
 package play.modules.mongodb

import org.asyncmongo.api._
import org.asyncmongo.protocol.commands._
import play.api._

class MongoAsyncPlugin(app :Application) extends Plugin {
	lazy val helper: MongoAsyncHelper = {
		val conf = MongoAsyncPlugin.parseConf(app)
		try {
			MongoAsyncHelper(conf._1, conf._2)
		} catch {
			case e => throw PlayException("MongoAsyncPlugin Initialization Error", "An exception occurred while initializing the MongoAsyncPlugin.", Some(e))
		}
	}

	def db: DB = helper.db
	def dbName: String = helper.dbName
	def connection: MongoConnection = helper.connection
	def collection(name :String): Collection = helper.db(name)

	override def onStart {
		Logger info "MongoAsyncPlugin starting..."
		Logger.info("MongoAsyncPlugin successfully started with db '%s'! Servers:\n\t\t%s"
			.format(
				helper.dbName, 
				helper.servers.map { s => "[%s]".format(s) }.mkString("\n\t\t")
			)
		)
	}
}

/**
* MongoDB access methods.
*/
object MongoAsyncPlugin {
	val DEFAULT_HOST = "localhost:27017"

	def connection(implicit app :Application) = current.connection
	def db(implicit app :Application) = current.db
	def collection(name :String)(implicit app :Application) = current.collection(name)
	def dbName(implicit app :Application) = current.dbName
	
	/** 
	  * returns the current instance of the plugin. 
	  */
	def current(implicit app :Application): MongoAsyncPlugin = app.plugin[MongoAsyncPlugin] match {
		case Some(plugin) => plugin
		case _ => throw PlayException("MongoAsyncPlugin Error", "The MongoAsyncPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.mongodb.MongoAsyncPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
	}

	/** 
	 * returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java). 
	 */
	def current(app :play.Application): MongoAsyncPlugin = app.plugin(classOf[MongoAsyncPlugin]) match {
		case plugin if plugin != null => plugin
		case _ => throw PlayException("MongoAsyncPlugin Error", "The MongoAsyncPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.mongodb.MongoAsyncPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
	}

	private def parseConf(app :Application): (String, List[String]) = {
		(
			app.configuration.getString("mongodb.db") match {
				case Some(db) => db
				case _ => throw app.configuration.globalError("Missing configuration key 'mongodb.db'!")
			},
			app.configuration.getStringList("mongodb.servers") match {
				case Some(list) => scala.collection.JavaConversions.collectionAsScalaIterable(list).toList
				case None => throw app.configuration.globalError("Missing configuration key 'mongodb.servers' (should be a list of servers)!")
			}
		)
	}
}

private[mongodb] case class MongoAsyncHelper(dbName: String, servers: List[String]) {
	lazy val connection = MongoConnection(servers)
	
	lazy val db = DB(dbName, connection)

	def collection(name :String): Collection = db(name)
}
