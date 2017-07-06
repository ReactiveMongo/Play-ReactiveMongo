import play.api.{ ApplicationLoader, Configuration, Environment, Mode }

import play.api.inject.guice.GuiceApplicationBuilder

object PlayUtil {
  def context = {
    val env = Environment.simple(mode = Mode.Test)

    ApplicationLoader.Context(env, None,
      new play.core.DefaultWebCommands(),
      Configuration.load(env),
      new play.api.inject.DefaultApplicationLifecycle())
  }

  def configure(initial: GuiceApplicationBuilder): GuiceApplicationBuilder =
    initial.load(
      new play.api.i18n.I18nModule(),
      new play.api.mvc.CookiesModule(),
      new play.api.inject.BuiltinModule(),
      new play.modules.reactivemongo.ReactiveMongoModule()
    )
}
