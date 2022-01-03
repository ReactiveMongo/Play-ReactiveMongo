package play.modules.reactivemongo

object TestUtils {
  def bindingKey(name: String) = ReactiveMongoModule.key(name)

  @inline def rightMap[L, R1, R2](
      e: Either[L, R1]
    )(f: R1 => R2
    ): Either[L, R2] = Compat.rightMap(e)(f)

}
