package play.modules.reactivemongo

import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

import scala.reflect.ClassTag

import play.api.data.FormError
import play.api.data.format.Formatter

import reactivemongo.api.bson._

import reactivemongo.play.json.compat.ValueConverters

/** Instances of [[https://www.playframework.com/documentation/latest/api/scala/index.html#play.api.data.format.Formatter Play Formatter]] for the ReactiveMongo types. */
object Formatters { self =>
  import play.api.libs.json.Json

  type Result[T] = Either[Seq[FormError], T]

  private def bind[T](key: String, data: Map[String, String])(f: String => Result[T]): Result[T] = data.get(key).fold[Result[T]](
    Left(Seq(FormError(key, "error.required", Nil))))(f)

  /** Formats BSON value as JSON. */
  implicit def bsonFormatter[T <: BSONValue](implicit cls: ClassTag[T]): Formatter[T] = new Formatter[T] {
    def bind(key: String, data: Map[String, String]): Result[T] =
      self.bind[T](key, data) { str =>
        try {
          ValueConverters.toValue(Json parse str) match {
            case `cls`(v) =>
              Right(v)

            case unexpected =>
              Left(Seq(FormError(
                key, s"Unexpected BSONValue: $unexpected", Nil)))
          }
        } catch {
          case NonFatal(cause) =>
            Left(Seq(FormError(
              key,
              s"fails to parse the JSON representation: $cause", Nil)))
        }
      }

    def unbind(key: String, value: T): Map[String, String] =
      Map(key -> Json.stringify(ValueConverters.fromValue(value)))
  }

  implicit object NumberLikeFormatter extends Formatter[BSONNumberLike] {
    def bind(key: String, data: Map[String, String]): Result[BSONNumberLike] =
      self.bind[BSONNumberLike](key, data) { str =>
        try {
          ValueConverters.toValue(Json parse str) match {
            case n: BSONNumberLike =>
              Right(n)

            case _ =>
              Left(Seq(FormError(key, "error.jsnumber.expected", str)))
          }
        } catch {
          case NonFatal(_) =>
            Left(Seq(FormError(key, "error.jsnumber.expected", str)))
        }
      }

    def unbind(key: String, value: BSONNumberLike): Map[String, String] =
      value.toDouble match {
        case Success(d) => {
          val n = BigDecimal(d)

          val json = {
            if (!n.ulp.isWhole) Json.toJson(d)
            else if (n.isValidInt) Json.toJson(d.toInt)
            else Json.toJson(d.toLong)
          }

          Map(key -> Json.stringify(json))
        }

        case _ => value.toLong match {
          case Success(l) => {
            val json = {
              if (l.isValidInt) Json.toJson(l.toInt)
              else Json.toJson(l)
            }

            Map(key -> Json.stringify(json))
          }

          case Failure(cause) =>
            throw cause
        }
      }
  }

  implicit object BooleanLikeFormatter extends Formatter[BSONBooleanLike] {
    def bind(key: String, data: Map[String, String]): Result[BSONBooleanLike] =
      self.bind[BSONBooleanLike](key, data) { str =>
        try {
          ValueConverters.toValue(Json parse str) match {
            case b: BSONBooleanLike =>
              Right(b)

            case _ =>
              Left(Seq(FormError(key, "error.jsboolean.expected", str)))
          }
        } catch {
          case NonFatal(_) =>
            Left(Seq(FormError(key, "error.jsboolean.expected", str)))
        }
      }

    def unbind(key: String, value: BSONBooleanLike): Map[String, String] =
      value.toBoolean match {
        case Success(b) =>
          Map(key -> Json.stringify(Json toJson b))

        case Failure(cause) =>
          throw cause
      }
  }
}
