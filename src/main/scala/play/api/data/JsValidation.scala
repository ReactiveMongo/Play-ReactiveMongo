package play.api.data.resource

import play.api.libs.json._
import play.api.libs.json.LensConstructor._
import play.api.data.validation._


sealed trait JsValidationResult[T] {
  def fold[X](
    invalid: Seq[JsLensValidationError] => X, 
    valid: T => X) = this match {
    case JsValidationSuccess(v) => valid(v)
    case JsValidationError(e) => invalid(e)
  }

  def map[X](f: T => X): JsValidationResult[X] = this match {
    case JsValidationSuccess(v) => JsValidationSuccess(f(v))
    case JsValidationError(e) => JsValidationError[X](e)
  }

  def flatMap[X](f: T => JsValidationResult[X]): JsValidationResult[X] = this match {
    case JsValidationSuccess(v) => f(v)
    case JsValidationError(e) => JsValidationError[X](e)
  }

}

case class JsValidationSuccess[T](value: T) extends JsValidationResult[T]

case class JsLensValidationError(lens: JsLens, message: String, args: Any*)
case class JsValidationError[T](errors: Seq[JsLensValidationError]) extends JsValidationResult[T]

trait Validates[T] {
  self => 

  def validates(json: JsValue, lens: JsLens = JsLens.identity): JsValidationResult[T]

  def checking[A](other: Validates[A]) = {
    new Validates[T] {
      def validates(json: JsValue, rootLens: JsLens = JsLens.identity): JsValidationResult[T] = {
        val otherVal = other.validates(json, rootLens)
        val selfVal = self.validates(json, rootLens)
        (selfVal, otherVal) match {
          case (JsValidationError(errors), JsValidationSuccess(_)) => JsValidationError[T](errors)
          case (JsValidationSuccess(_), JsValidationError(errors)) => JsValidationError[T](errors)
          case (JsValidationSuccess(t), JsValidationSuccess(_)) => JsValidationSuccess(t)
          case (JsValidationError(errors), JsValidationError(errors2)) => JsValidationError[T](errors ++ errors2)
        }
      }
    }
  }

  def and[V](other: Validates[V]): Validates[(T, V)] = new Validates[(T, V)] {
    def validates(json: JsValue, rootLens: JsLens = JsLens.identity): JsValidationResult[(T, V)] = {
      val selfVal = self.validates(json, rootLens)
      val otherVal = other.validates(json, rootLens)
      (selfVal, otherVal) match {
        case (JsValidationError(errors), JsValidationSuccess(_)) => JsValidationError[(T, V)](errors)
        case (JsValidationSuccess(_), JsValidationError(errors)) => JsValidationError[(T, V)](errors)
        case (JsValidationSuccess(t), JsValidationSuccess(v)) => JsValidationSuccess((t, v))
        case (JsValidationError(errors), JsValidationError(errors2)) => JsValidationError[(T, V)](errors ++ errors2)
      }
    }
  }
}

object Validators {
  implicit object StringValidates extends Validates[String] {
    def validates(json: JsValue, lens: JsLens = JsLens.identity): JsValidationResult[String] = lens(json) match {
      case JsString(s) => JsValidationSuccess(s)
      case _ => JsValidationError(Seq(JsLensValidationError(lens, "validate.error.expected.string")))
    }
  }

  implicit object IntValidates extends Validates[Int] {
    def validates(json: JsValue, lens: JsLens = JsLens.identity): JsValidationResult[Int] = lens(json) match {
      case JsNumber(s) => JsValidationSuccess(s.toInt)
      case _ => JsValidationError(Seq(JsLensValidationError(lens, "validate.error.expected.int")))
    }
  }

  implicit object DefaultJsValidates extends Validates[JsValue] {
    def validates(json: JsValue, lens: JsLens = JsLens.identity): JsValidationResult[JsValue] = lens(json) match {
      case JsUndefined(e) => JsValidationError[JsValue](Seq(JsLensValidationError(lens, e)))
      case js => JsValidationSuccess(js)
    }
  }
}

case class JsLensValidates[T](lens: JsLens, constraints: Seq[Constraint[T]] = Nil)(implicit valT: Validates[T]) extends Validates[T] {
  def validates(json: JsValue, rootLens: JsLens = JsLens.identity): JsValidationResult[T] = {
    val l = rootLens.andThen(lens)
    l.get(json) match {
      case JsUndefined(e) => JsValidationError[T](Seq(JsLensValidationError(l, e)))
      case js => valT.validates(js).flatMap( t => applyConstraints(l, t) )
    }
  }

  protected def applyConstraints(lens: JsLens, t: T): JsValidationResult[T] = {
    Option(collectErrors(lens, t))
      .filterNot(_.isEmpty)
      .map{ errors => val res: JsValidationResult[T] = JsValidationError(errors); res }
      .getOrElse( JsValidationSuccess(t))
  }

  protected def collectErrors(lens: JsLens, t: T): Seq[JsLensValidationError] = {
    constraints.map(_(t)).collect {
      case Invalid(errors) => errors.map{ e => JsLensValidationError(lens, e.message, e.args) }
    }.flatten
  }
}

object JsLensValidates {
  def apply[T](m: (JsLens, Constraint[T]))(implicit valT: Validates[T]) = new JsLensValidates[T](m._1, Seq(m._2))
}

object Validates {
  def apply[T, A1](c1: JsLensValidates[A1])
                  (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
                  (implicit valA1: Validates[A1]) = {
    new Validates[T]{
      def validates(json: JsValue, rootLens: JsLens = JsLens.identity): JsValidationResult[T] = {
        c1.validates(json, rootLens).map( a1 => apply(a1) )
      }
    }
  }

  def apply[T, A1, A2](c1: JsLensValidates[A1], c2: JsLensValidates[A2])
                      (apply: Function2[A1, A2, T])(unapply: Function1[T, Option[(A1, A2)]])
                      (implicit valA1: Validates[A1], valA2: Validates[A2]) = {
    new Validates[T]{
      def validates(json: JsValue, rootLens: JsLens = JsLens.identity): JsValidationResult[T] = {
        (c1 and c2).validates(json, rootLens).map{ case (a1, a2) => apply(a1, a2) }
      }
    }
  }
}

object ValidateWrites {
  def apply[T, A1](c1: JsLensValidates[A1])
                  (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
                  (implicit wA1: Writes[A1]) = {
    new Writes[T]{
      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(product) =>
            c1.lens.set(JsObject(Seq()), Json.toJson(product))
          case _ => throw new RuntimeException("product expected")
        }
      }
    }
  }

  def apply[T, A1, A2](c1: JsLensValidates[A1], c2: JsLensValidates[A2])
                      (apply: Function2[A1, A2, T])(unapply: Function1[T, Option[(A1, A2)]])
                      (implicit wA1: Writes[A1], wA2: Writes[A2]) = {
    new Writes[T]{
      def writes(t: T): JsValue = {
        unapply(t) match {
          case Some(product) =>
            c2.lens.set(c1.lens.set(JsObject(Seq()), Json.toJson(product._1)), Json.toJson(product._2))
          case _ => throw new RuntimeException("product expected")
        }
      }
    }
  }
}