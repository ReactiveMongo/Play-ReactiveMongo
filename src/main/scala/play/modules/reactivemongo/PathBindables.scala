package play.modules.reactivemongo

import scala.util.{ Failure, Success }

import play.api.mvc.PathBindable

import reactivemongo.bson._

import reactivemongo.api.bson.{
  BSONDateTime => BisonDateTime,
  BSONDouble => BisonDouble,
  BSONBoolean => BisonBoolean,
  BSONLong => BisonLong,
  BSONString => BisonString,
  BSONSymbol => BisonSymbol,
  BSONTimestamp => BisonTimestamp,
  BSONObjectID => BisonObjectID
}

/** Instances of [[https://www.playframework.com/documentation/latest/api/scala/index.html#play.api.mvc.PathBindable Play PathBindable]] for the ReactiveMongo types. */
object PathBindables {
  import play.modules.reactivemongo.Compat.{ rightFlatMap, rightMap }

  @deprecated("Use reactivemongo.api.bson.BSONBoolean", "0.20.4")
  implicit object BSONBooleanPathBindable extends PathBindable[BSONBoolean] {
    private val b = implicitly[PathBindable[Boolean]]

    def bind(key: String, value: String): Either[String, BSONBoolean] =
      rightMap(b.bind(key, value))(BSONBoolean(_))

    def unbind(key: String, value: BSONBoolean): String =
      b.unbind(key, value.value)
  }

  implicit object BSONBisonPathBindable extends PathBindable[BisonBoolean] {
    private val b = implicitly[PathBindable[Boolean]]

    def bind(key: String, value: String): Either[String, BisonBoolean] =
      rightMap(b.bind(key, value))(BisonBoolean(_))

    def unbind(key: String, value: BisonBoolean): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONDateTime", "0.20.4")
  implicit object BSONDateTimePathBindable extends PathBindable[BSONDateTime] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONDateTime] =
      rightMap(b.bind(key, value))(BSONDateTime(_))

    def unbind(key: String, value: BSONDateTime): String =
      b.unbind(key, value.value)
  }

  implicit object BisonDateTimePathBindable extends PathBindable[BisonDateTime] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BisonDateTime] =
      rightMap(b.bind(key, value))(BisonDateTime(_))

    def unbind(key: String, value: BisonDateTime): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONDouble", "0.20.4")
  implicit object BSONDoublePathBindable extends PathBindable[BSONDouble] {
    val b = implicitly[PathBindable[Double]]

    def bind(key: String, value: String): Either[String, BSONDouble] =
      rightMap(b.bind(key, value))(BSONDouble(_))

    def unbind(key: String, value: BSONDouble): String =
      b.unbind(key, value.value)
  }

  implicit object BisonDoublePathBindable extends PathBindable[BisonDouble] {
    val b = implicitly[PathBindable[Double]]

    def bind(key: String, value: String): Either[String, BisonDouble] =
      rightMap(b.bind(key, value))(BisonDouble(_))

    def unbind(key: String, value: BisonDouble): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONLong", "0.20.4")
  implicit object BSONLongPathBindable extends PathBindable[BSONLong] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONLong] =
      rightMap(b.bind(key, value))(BSONLong(_))

    def unbind(key: String, value: BSONLong): String =
      b.unbind(key, value.value)
  }

  implicit object BisonLongPathBindable extends PathBindable[BisonLong] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BisonLong] =
      rightMap(b.bind(key, value))(BisonLong(_))

    def unbind(key: String, value: BisonLong): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONString", "0.20.4")
  implicit object BSONStringPathBindable extends PathBindable[BSONString] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONString] =
      rightMap(b.bind(key, value))(BSONString(_))

    def unbind(key: String, value: BSONString): String =
      b.unbind(key, value.value)
  }

  implicit object BisonStringPathBindable extends PathBindable[BisonString] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BisonString] =
      rightMap(b.bind(key, value))(BisonString(_))

    def unbind(key: String, value: BisonString): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONSymbol", "0.20.4")
  implicit object BSONSymbolPathBindable extends PathBindable[BSONSymbol] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONSymbol] =
      rightMap(b.bind(key, value))(BSONSymbol(_))

    def unbind(key: String, value: BSONSymbol): String =
      b.unbind(key, value.value)
  }

  implicit object BisonSymbolPathBindable extends PathBindable[BisonSymbol] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BisonSymbol] =
      rightMap(b.bind(key, value))(BisonSymbol(_))

    def unbind(key: String, value: BisonSymbol): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONTimestamp", "0.20.4")
  implicit object BSONTimestampPathBindable extends PathBindable[BSONTimestamp] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONTimestamp] =
      rightMap(b.bind(key, value))(BSONTimestamp(_))

    def unbind(key: String, value: BSONTimestamp): String =
      b.unbind(key, value.value)
  }

  implicit object BisonTimestampPathBindable extends PathBindable[BisonTimestamp] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BisonTimestamp] =
      rightMap(b.bind(key, value))(BisonTimestamp(_))

    def unbind(key: String, value: BisonTimestamp): String =
      b.unbind(key, value.value)
  }

  @deprecated("Use reactivemongo.api.bson.BSONObjectID", "0.20.4")
  implicit object BSONObjectIDPathBindable extends PathBindable[BSONObjectID] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONObjectID] =
      rightFlatMap(b.bind(key, value)) { str =>
        BSONObjectID.parse(str) match {
          case Failure(cause) =>
            Left(Option(cause.getMessage).getOrElse(cause.toString))

          case Success(oid) => Right(oid)
        }
      }

    def unbind(key: String, value: BSONObjectID): String =
      b.unbind(key, value.stringify)
  }

  implicit object BisonObjectIDPathBindable extends PathBindable[BisonObjectID] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BisonObjectID] =
      rightFlatMap(b.bind(key, value)) { str =>
        BisonObjectID.parse(str) match {
          case Failure(cause) =>
            Left(Option(cause.getMessage).getOrElse(cause.toString))

          case Success(oid) => Right(oid)
        }
      }

    def unbind(key: String, value: BisonObjectID): String =
      b.unbind(key, value.stringify)
  }
}
