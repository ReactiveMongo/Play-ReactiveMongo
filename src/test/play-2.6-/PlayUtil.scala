import play.api.{ ApplicationLoader, Configuration, Environment, Mode }
import play.api.inject.guice.GuiceApplicationBuilder

object PlayUtil {

  def context = {
    val env = Environment.simple(mode = Mode.Test)

    ApplicationLoader.Context(
      env,
      None,
      new play.core.DefaultWebCommands(),
      Configuration.load(env)
    )
  }

  def stringList(config: Configuration, key: String) = config.getStringList(key)

  def configure(initial: GuiceApplicationBuilder): GuiceApplicationBuilder =
    initial.load(
      new play.api.inject.BuiltinModule(),
      new play.modules.reactivemongo.ReactiveMongoModule()
    )
}
