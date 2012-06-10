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
import org.asyncmongo.actors.MongoConnection
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
	def connection: MongoConnection = helper.connection
	def collection(name :String): Collection = helper.db(name)

	override def onStart {
		Logger info "MongoAsyncPlugin starting..."
		Logger.info("MongoAsyncPlugin successfully started with db '%s'! Servers:\n\t\t%s"
			.format(
				helper.dbName, 
				helper.servers.map { s => "[%s:%s]".format(s._1, s._2) }.mkString("\n\t\t")
			)
		)
	}
}

/**
* MongoDB access methods.
*/
object MongoAsyncPlugin {
	val DEFAULT_HOST = "localhost"
	val DEFAULT_PORT = 27017

	def connection(implicit app :Application) = current.connection
	def db(implicit app :Application) = current.db
	def collection(name :String)(implicit app :Application) = current.collection(name)

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

	private def parseConf(app :Application): (String, List[(String, Int)]) = {
		(
			app.configuration.getString("mongodb.db") match {
				case Some(db) => db
				case _ => throw app.configuration.globalError("Missing configuration key 'mongodb.db'!")
			}, 	
			app.configuration.getConfig("mongodb.servers") match {
				case Some(config) =>
					config.keys.toList.sorted.map(_.span(_ != '.')._1).map { address: String =>
						val hostport = address.span( _ != ':' )

						(	
							if(hostport._1.isEmpty) DEFAULT_HOST else hostport._1,
							if(hostport._2.isEmpty) DEFAULT_PORT else hostport._2.toInt
						)
										
					}
				case _ => 
					List((app.configuration.getString("mongodb.host").getOrElse(DEFAULT_HOST), 
					app.configuration.getInt("mongodb.port").getOrElse(DEFAULT_PORT)))
			}		
		)
	}
}

private[mongodb] case class MongoAsyncHelper(dbName: String, servers: List[(String, Int)]) {
	lazy val connection = MongoConnection(servers)
	
	lazy val db = DB(dbName, connection)

	def collection(name :String): Collection = db(name)
}


