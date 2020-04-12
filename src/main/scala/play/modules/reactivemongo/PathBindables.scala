package play.modules.reactivemongo

import scala.util.{ Failure, Success }

import play.api.mvc.PathBindable

import reactivemongo.api.bson.{
  BSONDateTime,
  BSONDouble,
  BSONBoolean,
  BSONLong,
  BSONString,
  BSONSymbol,
  BSONTimestamp,
  BSONObjectID
}

/** Instances of [[https://www.playframework.com/documentation/latest/api/scala/index.html#play.api.mvc.PathBindable Play PathBindable]] for the ReactiveMongo types. */
object PathBindables {
  import play.modules.reactivemongo.Compat.{ rightFlatMap, rightMap }

  implicit object BSONBSONPathBindable extends PathBindable[BSONBoolean] {
    private val b = implicitly[PathBindable[Boolean]]

    def bind(key: String, value: String): Either[String, BSONBoolean] =
      rightMap(b.bind(key, value))(BSONBoolean(_))

    def unbind(key: String, value: BSONBoolean): String =
      b.unbind(key, value.value)
  }

  implicit object BSONDateTimePathBindable extends PathBindable[BSONDateTime] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONDateTime] =
      rightMap(b.bind(key, value))(BSONDateTime(_))

    def unbind(key: String, value: BSONDateTime): String =
      b.unbind(key, value.value)
  }

  implicit object BSONDoublePathBindable extends PathBindable[BSONDouble] {
    val b = implicitly[PathBindable[Double]]

    def bind(key: String, value: String): Either[String, BSONDouble] =
      rightMap(b.bind(key, value))(BSONDouble(_))

    def unbind(key: String, value: BSONDouble): String =
      b.unbind(key, value.value)
  }

  implicit object BSONLongPathBindable extends PathBindable[BSONLong] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONLong] =
      rightMap(b.bind(key, value))(BSONLong(_))

    def unbind(key: String, value: BSONLong): String =
      b.unbind(key, value.value)
  }

  implicit object BSONStringPathBindable extends PathBindable[BSONString] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONString] =
      rightMap(b.bind(key, value))(BSONString(_))

    def unbind(key: String, value: BSONString): String =
      b.unbind(key, value.value)
  }

  implicit object BSONSymbolPathBindable extends PathBindable[BSONSymbol] {
    val b = implicitly[PathBindable[String]]

    def bind(key: String, value: String): Either[String, BSONSymbol] =
      rightMap(b.bind(key, value))(BSONSymbol(_))

    def unbind(key: String, value: BSONSymbol): String =
      b.unbind(key, value.value)
  }

  implicit object BSONTimestampPathBindable extends PathBindable[BSONTimestamp] {
    val b = implicitly[PathBindable[Long]]

    def bind(key: String, value: String): Either[String, BSONTimestamp] =
      rightMap(b.bind(key, value))(BSONTimestamp(_))

    def unbind(key: String, value: BSONTimestamp): String =
      b.unbind(key, value.value)
  }

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
}
