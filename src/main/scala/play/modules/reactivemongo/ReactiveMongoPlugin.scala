/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package play.modules.reactivemongo

import play.api._
import uk.gov.hmrc.mongo.MongoConnector
import reactivemongo.api.FailoverStrategy
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit


class ReactiveMongoPlugin(app: Application) extends Plugin {
  private var _mongoConnector: Option[MongoConnector] = None

  def mongoConnector: MongoConnector = _mongoConnector.getOrElse(throw new Exception("ReactiveMongoPlugin error: no MongoConnector available?"))

  override def onStart {
    Logger info "ReactiveMongoPlugin starting..."
    _mongoConnector = Some(ReactiveMongoPlugin.parseConf(app))
  }

  override def onStop {
    Logger.info("ReactiveMongoPlugin stops, closing connections...")
    _mongoConnector.map {
      h => h.close()
    }
    _mongoConnector = None
  }
}

/**
 * MongoDB access methods.
 */
object ReactiveMongoPlugin {

  def mongoConnector(implicit app: Application) = current.mongoConnector

  /** Returns the current instance of the plugin. */
  def current(implicit app: Application): ReactiveMongoPlugin = app.plugin[ReactiveMongoPlugin] match {
    case Some(plugin) => plugin
    case _ => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  /** Returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java). */
  def current(app: play.Application): ReactiveMongoPlugin = app.plugin(classOf[ReactiveMongoPlugin]) match {
    case plugin if plugin != null => plugin
    case _ => throw new PlayException("ReactiveMongoPlugin Error", "The ReactiveMongoPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.reactivemongo.ReactiveMongoPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  private [reactivemongo] def parseConf(app: Application): MongoConnector = {

    val mongoConfig = app.configuration.getConfig("mongodb")
      .getOrElse(app.configuration.getConfig(s"${app.mode}.mongodb")
      .getOrElse(app.configuration.getConfig(s"${Mode.Dev}.mongodb")
      .getOrElse(throw new Exception("The application does not contain required mongodb configuration"))))

    mongoConfig.getString("uri") match {
      case Some(uri) => {

        mongoConfig.getInt("channels").foreach { _ =>
          Logger.warn("the mongodb.channels configuration key has been removed and is now ignored. Please use the mongodb URL option described here: https://docs.mongodb.org/manual/reference/connection-string/#connections-connection-options. https://github.com/ReactiveMongo/ReactiveMongo/blob/0.11.3/driver/src/main/scala/api/api.scala#L577")
        }

        val failoverStrategy: Option[FailoverStrategy] = mongoConfig.getConfig("failoverStrategy") match {
          case Some(fs: Configuration) => {

            val initialDelay: FiniteDuration = fs.getLong("initialDelayMsecs").map(delay => new FiniteDuration(delay, TimeUnit.MILLISECONDS)).getOrElse(FailoverStrategy().initialDelay)
            val retries: Int = fs.getInt("retries").getOrElse(FailoverStrategy().retries)

            Some(FailoverStrategy().copy(initialDelay = initialDelay, retries = retries, delayFactor = DelayFactor(fs.getConfig("delay"))))
          }
          case _ => None
        }

        new MongoConnector(uri, failoverStrategy)
      }
      case _ => throw new Exception("No MongoDB URI configuration found")
    }
  }

}

private [reactivemongo] object DelayFactor {

  import scala.math.pow

  def apply(delay : Option[Configuration]) : (Int) => Double = {
    delay match {
      case Some(df: Configuration) => {

        val delayFactor = df.getDouble("factor").getOrElse(1.0)

        df.getString("function") match {
          case Some("linear") => linear(delayFactor)
          case Some("exponential") => exponential(delayFactor)
          case Some("static") => static(delayFactor)
          case Some("fibonacci") => fibonacci(delayFactor)
          case unsupported => throw new PlayException("ReactiveMongoPlugin Error", s"Invalid Mongo configuration for delay function: unknown '$unsupported' function")
        }
      }
      case _ => FailoverStrategy().delayFactor
    }
  }

  private def linear(f: Double): Int => Double = n => n * f

  private def exponential(f: Double): Int => Double = n => pow(n, f)

  private def static(f: Double): Int => Double = n => f

  private def fibonacci(f: Double): Int => Double = n => f * (fib take n).last

  def fib: Stream[Long] = {
    def tail(h: Long, n: Long): Stream[Long] = h #:: tail(n, h + n)
    tail(0, 1)
  }
}
