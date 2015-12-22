package play.modules.reactivemongo

import javax.inject._

import akka.actor.ActorSystem
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.{ ApplicationLifecycle, Binding, Module }

/**
 * MongoDB module.
 */
@Singleton
final class ReactiveMongoModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(bind[ReactiveMongoApi].to[DefaultReactiveMongoApi].in[Singleton])

}

/**
 * Cake pattern components.
 */
trait ReactiveMongoComponents {
  def reactiveMongoApi: ReactiveMongoApi
}

/**
 * Components for compile time injection
 */
trait ReactiveMongoApiComponents {
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle
  def actorSystem: ActorSystem

  lazy val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(actorSystem, configuration, applicationLifecycle)
}