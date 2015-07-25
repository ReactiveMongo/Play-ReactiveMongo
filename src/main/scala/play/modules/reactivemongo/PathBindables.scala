package play.modules.reactivemongo

import play.api.mvc.PathBindable

import reactivemongo.bson._

/** Instances of [[https://www.playframework.com/documentation/2.4.0/api/scala/index.html#play.api.mvc.PathBindable Play PathBindable]] for the ReactiveMongo types. */
object PathBindables {
  implicit object BSONBooleanPathBindable extends PathBindable[BSONBoolean] {
    private val b = implicitly[PathBindable[Boolean]]

    def bind(key: String, value: String): Either[String, BSONBoolean] =
      b.bind(key, value).right.map(BSONBoolean(_))

    def unbind(key: String, value: BSONBoolean): String =
      b.unbind(key, value.value)
  }

  implicit object BSONDateTimePathBindable extends PathBindable[BSONDateTime] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONDateTime] =
      b.bind(key, value).right.map(BSONDateTime(_))

    def unbind(key: String, value: BSONDateTime): String =
      b.unbind(key, value.value)
  }

  implicit object BSONDoublePathBindable extends PathBindable[BSONDouble] {
    val b = implicitly[PathBindable[Double]]

    def bind(key: String, value: String): Either[String, BSONDouble] =
      b.bind(key, value).right.map(BSONDouble(_))

    def unbind(key: String, value: BSONDouble): String =
      b.unbind(key, value.value)
  }

  implicit object BSONLongPathBindable extends PathBindable[BSONLong] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONLong] =
      b.bind(key, value).right.map(BSONLong(_))

    def unbind(key: String, value: BSONLong): String =
      b.unbind(key, value.value)
  }

  implicit object BSONStringPathBindable extends PathBindable[BSONString] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONString] =
      b.bind(key, value).right.map(BSONString(_))

    def unbind(key: String, value: BSONString): String =
      b.unbind(key, value.value)
  }

  implicit object BSONSymbolPathBindable extends PathBindable[BSONSymbol] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONSymbol] =
      b.bind(key, value).right.map(BSONSymbol(_))

    def unbind(key: String, value: BSONSymbol): String =
      b.unbind(key, value.value)
  }

  implicit object BSONTimestampPathBindable extends PathBindable[BSONTimestamp] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONTimestamp] =
      b.bind(key, value).right.map(BSONTimestamp(_))

    def unbind(key: String, value: BSONTimestamp): String =
      b.unbind(key, value.value)
  }

  implicit object BSONObjectIDPathBindable extends PathBindable[BSONObjectID] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONObjectID] =
      b.bind(key, value).right.map(BSONObjectID(_))

    def unbind(key: String, value: BSONObjectID): String =
      b.unbind(key, value.stringify)
  }
}
