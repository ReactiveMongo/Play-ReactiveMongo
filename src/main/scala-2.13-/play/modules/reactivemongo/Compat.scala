package play.modules.reactivemongo

private[reactivemongo] object Compat {

  @inline def rightMap[L, R1, R2](
      e: Either[L, R1]
    )(f: R1 => R2
    ): Either[L, R2] = e.right.map(f)

  @inline def rightFlatMap[L, R1, R2](
      e: Either[L, R1]
    )(f: R1 => Either[L, R2]
    ): Either[L, R2] = e.right.flatMap(f)
}
