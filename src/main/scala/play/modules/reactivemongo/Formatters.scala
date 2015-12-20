package play.modules.reactivemongo

import scala.util.{ Failure, Success }

import reactivemongo.bson._
import reactivemongo.play.json.BSONFormats

import play.api.data.FormError
import play.api.data.format.Formatter

/** Instances of [[https://www.playframework.com/documentation/2.4.0/api/scala/index.html#play.api.data.format.Formatter Play Formatter]] for the ReactiveMongo types. */
object Formatters { self =>
  import play.api.libs.json.{ Format, Json, JsSuccess }

  type Result[T] = Either[Seq[FormError], T]

  private def bind[T](key: String, data: Map[String, String])(f: String => Result[T]): Result[T] = data.get(key).fold[Result[T]](
    Left(Seq(FormError(key, "error.required", Nil))))(f)

  /** Formats BSON value as JSON. */
  implicit def bsonFormatter[T <: BSONValue: Format]: Formatter[T] =
    new Formatter[T] {
      import play.api.libs.json.JsError

      private val jsonFormat = implicitly[Format[T]]

      def bind(key: String, data: Map[String, String]): Result[T] =
        self.bind[T](key, data) { str =>
          jsonFormat.reads(Json.parse(str)) match {
            case JsSuccess(bson, _) => Right(bson)
            case err @ JsError(_) => Left(Seq(FormError(key,
              s"fails to parse the JSON representation: $err", Nil)))
          }
        }

      def unbind(key: String, value: T): Map[String, String] =
        Map(key -> Json.stringify(Json.toJson(value)(jsonFormat)))
    }

  implicit object NumberLikeFormatter extends Formatter[BSONNumberLike] {
    import play.api.libs.json.JsNumber
    import BSONNumberLike._

    def bind(key: String, data: Map[String, String]): Result[BSONNumberLike] =
      self.bind[BSONNumberLike](key, data) { str =>
        BSONFormats.numberReads.lift(Json.parse(str)) match {
          case Some(JsSuccess(d @ BSONDouble(_), _)) =>
            Right(new BSONDoubleNumberLike(d))

          case Some(JsSuccess(i @ BSONInteger(_), _)) =>
            Right(new BSONIntegerNumberLike(i))

          case Some(JsSuccess(l @ BSONLong(_), _)) =>
            Right(new BSONLongNumberLike(l))

          case _ =>
            Left(Seq(FormError(key, "error.jsnumber.expected", str)))
        }
      }

    def unbind(key: String, value: BSONNumberLike): Map[String, String] = {
      val n = BigDecimal(value.toDouble)
      val json =
        if (!n.ulp.isWhole) Json.toJson(value.toDouble)
        else if (n.isValidInt) Json.toJson(value.toInt)
        else Json.toJson(value.toLong)

      Map(key -> Json.stringify(json))
    }
  }

  implicit object BooleanLikeFormatter extends Formatter[BSONBooleanLike] {
    import play.api.libs.json.JsBoolean
    import BSONBooleanLike._

    def bind(key: String, data: Map[String, String]): Result[BSONBooleanLike] =
      self.bind[BSONBooleanLike](key, data) { str =>
        BSONFormats.BSONBooleanFormat.
          partialReads.lift(Json.parse(str)) match {
            case Some(JsSuccess(b @ BSONBoolean(_), _)) =>
              Right(new BSONBooleanBooleanLike(b))

            case _ =>
              Left(Seq(FormError(key, "error.jsboolean.expected", str)))
          }
      }

    def unbind(key: String, value: BSONBooleanLike): Map[String, String] =
      Map(key -> Json.stringify(Json toJson value.toBoolean))

  }
}
