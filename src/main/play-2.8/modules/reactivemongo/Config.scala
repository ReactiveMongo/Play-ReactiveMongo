package play.modules.reactivemongo

import play.api.Configuration

private[reactivemongo] object Config {
  @inline def configuration(underlying: Configuration)(key: String) =
    underlying.getOptional[Configuration](key)

  @inline def string(underlying: Configuration)(key: String) =
    underlying.getOptional[String](key)

  @inline def boolean(underlying: Configuration)(key: String) =
    underlying.getOptional[Boolean](key)
}
