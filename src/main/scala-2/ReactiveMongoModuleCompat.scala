package play.modules.reactivemongo

import play.api.inject.BindingKey

private[reactivemongo] trait ReactiveMongoModuleCompat {
  _: ReactiveMongoModule.type =>

  private[reactivemongo] def key(name: String): BindingKey[ReactiveMongoApi] =
    BindingKey(classOf[ReactiveMongoApi])
      .qualifiedWith(new NamedDatabaseImpl(name))

}
