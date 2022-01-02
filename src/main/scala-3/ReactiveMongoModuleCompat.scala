package play.modules.reactivemongo

import play.api.inject.BindingKey

// TODO: Remove when NamedDatabase annotation is supported property
private[reactivemongo] trait ReactiveMongoModuleCompat {
  _self: ReactiveMongoModule.type =>

  private[reactivemongo] def key(name: String): BindingKey[ReactiveMongoApi] =
    BindingKey(classOf[ReactiveMongoApi])

}
