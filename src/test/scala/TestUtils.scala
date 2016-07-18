package play.modules.reactivemongo

object TestUtils {
  def bindingKey(name: String) = ReactiveMongoModule.key(name)
}
