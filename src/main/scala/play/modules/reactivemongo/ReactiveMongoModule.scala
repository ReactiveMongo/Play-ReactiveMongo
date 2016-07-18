package play.modules.reactivemongo

import javax.inject._

import play.api._
import play.api.inject.{ ApplicationLifecycle, Binding, BindingKey, Module }

/**
 * MongoDB module.
 */
@Singleton
final class ReactiveMongoModule extends Module {
  import DefaultReactiveMongoApi.BindingInfo

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = apiBindings(DefaultReactiveMongoApi.parseConfiguration(configuration), configuration)

  private def apiBindings(info: Seq[(String, BindingInfo)], cf: Configuration): Seq[Binding[ReactiveMongoApi]] = info.flatMap {
    case (name, BindingInfo(strict, db, uri)) =>
      val provider = new ReactiveMongoProvider(
        new DefaultReactiveMongoApi(name, uri, db, strict, cf, _)
      )
      val bs = List(ReactiveMongoModule.key(name).to(provider))

      if (name == "default") {
        bind[ReactiveMongoApi].to(provider) :: bs
      } else bs
  }
}

object ReactiveMongoModule {
  private[reactivemongo] def key(name: String): BindingKey[ReactiveMongoApi] =
    BindingKey(classOf[ReactiveMongoApi]).
      qualifiedWith(new NamedDatabaseImpl(name))

}

/**
 * Cake pattern components.
 */
trait ReactiveMongoComponents {
  def reactiveMongoApi: ReactiveMongoApi
}

/**
 * Inject provider for named databases.
 */
private[reactivemongo] final class ReactiveMongoProvider(
    factory: ApplicationLifecycle => ReactiveMongoApi
) extends Provider[ReactiveMongoApi] {
  @Inject private var applicationLifecycle: ApplicationLifecycle = _
  lazy val get: ReactiveMongoApi = factory(applicationLifecycle)
}
