package play.modules.reactivemongo

import play.api.Configuration

private[reactivemongo] object Config {

  @inline def configuration(underlying: Configuration)(key: String) =
    underlying.getConfig(key)

  @inline def string(underlying: Configuration)(key: String) =
    underlying.getString(key)

  @inline def boolean(underlying: Configuration)(key: String) =
    underlying.getBoolean(key)

}
