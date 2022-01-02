package play.modules.reactivemongo

import javax.inject._

import scala.concurrent.ExecutionContext

import play.api._
import play.api.inject.{ ApplicationLifecycle, Binding, Module }

/**
 * MongoDB module.
 */
@Singleton
final class ReactiveMongoModule extends Module {
  import DefaultReactiveMongoApi.BindingInfo

  override def bindings(
      environment: Environment,
      configuration: Configuration
    ): Seq[Binding[_]] = apiBindings(
    DefaultReactiveMongoApi.parseConfiguration(configuration)(
      ExecutionContext.global
    ),
    configuration
  )

  private def apiBindings(
      info: Seq[(String, BindingInfo)],
      cf: Configuration
    ): Seq[Binding[ReactiveMongoApi]] = info.flatMap {
    case (name, BindingInfo(strict, db, uri)) =>
      val provider = new ReactiveMongoProvider(
        new DefaultReactiveMongoApi(uri, db, strict, cf, _)(_)
      )

      val bs = List(ReactiveMongoModule.key(name).to(provider))

      if (name == "default") {
        bind[ReactiveMongoApi].to(provider) :: bs
      } else bs
  }
}

object ReactiveMongoModule extends ReactiveMongoModuleCompat

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
    factory: (ApplicationLifecycle, ExecutionContext) => ReactiveMongoApi)
    extends Provider[ReactiveMongoApi] {
  import com.github.ghik.silencer.silent

  @silent @Inject private var applicationLifecycle: ApplicationLifecycle = _

  @silent @Inject private var executionContext: ExecutionContext = _

  lazy val get: ReactiveMongoApi =
    factory(applicationLifecycle, executionContext)
}
