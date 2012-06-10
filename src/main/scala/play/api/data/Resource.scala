package play.api.data.resource

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.LensConstructor._
import play.api.data.validation._
import play.api.libs.concurrent.Promise

sealed trait ResourceResult[T] {
  def map[X](f: T => X): ResourceResult[X] = this match {
    case ResourceSuccess(t) => ResourceSuccess(f(t))
    case ResourceValidationError(e) => ResourceValidationError[X](e)
    case ResourceOpError(e) => ResourceOpError[X](e)
  }

  def flatMap[X](f: T => ResourceResult[X]): ResourceResult[X] = this match {
    case ResourceSuccess(t) => f(t)
    case ResourceValidationError(e) => ResourceValidationError[X](e)
    case ResourceOpError(e) => ResourceOpError[X](e)
  }

  def fold[X](
    errorValid: Seq[ResourceErrorMsg] => X, 
    errorOp: Seq[ResourceErrorMsg] => X, 
    success: T => X) = this match {
      case ResourceSuccess(v) => success(v)
      case ResourceValidationError(e) => errorValid(e)
      case ResourceOpError(e) => errorOp(e)
    }

  def foldValid[X](
    invalid: Seq[ResourceErrorMsg] => X, 
    valid: T => X) = this match {
      case ResourceSuccess(v) => valid(v)
      case ResourceValidationError(e) => invalid(e)
      case _ => sys.error("unexpected state")
    }

  def foldOp[X](
    error: Seq[ResourceErrorMsg] => X, 
    success: T => X) = this match {
      case ResourceSuccess(v) => success(v)
      case ResourceOpError(e) => error(e)
      case _ => sys.error("unexpected state")
    }
}

case class ResourceErrorMsg(key: String, message: String, args: Any*)

case class ResourceSuccess[T](value: T) extends ResourceResult[T]
case class ResourceValidationError[T](errors: Seq[ResourceErrorMsg]) extends ResourceResult[T]
case class ResourceOpError[T](errors: Seq[ResourceErrorMsg]) extends ResourceResult[T]

trait ResourceTemplate[T] {
  def insert(json: JsValue): Promise[ResourceResult[T]]
  def findOne(json: JsValue): Promise[ResourceResult[T]]
  //def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]]
  //def update(s: S): T
  //def delete(s: S)
  //def getBatch(s: Enumerator[S]): Enumerator[T]
}


class Resource[T](tmpl: ResourceTemplate[JsValue], 
                  inputTransform: JsValue => JsValue = identity, 
                  outputTransform: JsValue => JsValue = identity,
                  queryTransform: JsValue => JsValue = identity)
                  (implicit formatter: Validates[T], writer: Writes[T]) {

  def insert(json: JsValue): Promise[ResourceResult[T]] = {
    formatter.validates(json).fold(
      invalid = { e => Promise.pure(ResourceValidationError(e.map( e => ResourceErrorMsg(e.lens.toString, e.message, e.args:_*) ))) },
      valid = { s => 
        tmpl.insert(json).map( _.foldOp(
          error = { e => ResourceOpError(e.map( e => ResourceErrorMsg(e.key, e.message, e.args:_*) )) },
          success = { e => ResourceSuccess(s) }
        ))
      }
    )
  }

  def findOne(json: JsValue): Promise[ResourceResult[T]] = {
    tmpl.findOne(json).map( _.flatMap( js => 
      formatter.validates(js).fold[ResourceResult[T]](
        invalid = { e => ResourceValidationError(e.map( e => ResourceErrorMsg(e.lens.toString, e.message, e.args:_*) )) },
        valid = { t => ResourceSuccess(t) }
      )
    ))
  }


  /*def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]] = {
  }*/

  def checking[A](c: (JsLens, Constraint[A]))(implicit v:Validates[A]) = {
    new Resource(
      this.tmpl, 
      this.inputTransform, 
      this.outputTransform,
      this.queryTransform)(this.formatter.checking(JsLensValidates(c)), this.writer)
  }
  
  def transformInput( f: JsValue => JsValue ) = new Resource(this.tmpl, f, this.outputTransform, this.queryTransform)
  def transformOutput( f: JsValue => JsValue ) = new Resource(this.tmpl, this.inputTransform, f, this.queryTransform)
  def transformQuery( f: JsValue => JsValue ) = new Resource(this.tmpl, this.inputTransform, this.outputTransform, f)
}

object Resource {
  def apply[T](tmpl: ResourceTemplate[JsValue])(implicit v: Validates[T], w: Writes[T]) = new Resource[T](tmpl)

  def apply[T, A1](c1: (JsLens, Constraint[A1]))
                  (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
                  (tmpl: ResourceTemplate[JsValue])
                  (implicit valA1: Validates[A1], wA1: Writes[A1]): Resource[T] = {
    implicit val valT = Validates(JsLensValidates(c1))(apply)(unapply)
    implicit val wT = ValidateWrites(JsLensValidates(c1))(apply)(unapply)

    new Resource[T](tmpl)                
  }

  def apply[T, A1, A2](c1: (JsLens, Constraint[A1]), c2: (JsLens, Constraint[A2]))
                  (apply: Function2[A1, A2, T])(unapply: Function1[T, Option[(A1, A2)]])
                  (tmpl: ResourceTemplate[JsValue])
                  (implicit valA1: Validates[A1], valA2: Validates[A2], wA1: Writes[A1], wA2: Writes[A2]): Resource[T] = {
    implicit val valT = Validates(JsLensValidates(c1), JsLensValidates(c2))(apply)(unapply)
    implicit val wT = ValidateWrites(JsLensValidates(c1), JsLensValidates(c2))(apply)(unapply)

    new Resource[T](tmpl)                
  }

}



/**
 *
 * The Resource Controller to be plugged in your application
 *
 */
class ResourceController[T](res: Resource[T])(implicit valT: Validates[T], wT: Writes[T]) extends Controller {

  def insert = Action(parse.json) { implicit request =>
    Async {
      res.insert(request.body).map( _.fold(
        errorValid = { errors => BadRequest(errors.toString) },
        errorOp = { errors => BadRequest(errors.toString) },
        success = { value => Ok("inserted " + value) }
      ))
    }
  } 

  def findOne(q: String) = Action {implicit request =>
    val json = Json.parse(q)
    Async {
      /*val json = request.queryString.foldLeft(JsObject(Nil))( (all: JsObject, elt: (String, Seq[String])) => 
        all ++ (if(elt._2.length == 1 ) Json.obj(elt._1 -> Json.toJson(elt._2(0))) else Json.obj(elt._1 -> Json.toJson(elt._2)))
      )*/
      res.findOne(json).map( _.fold(
        errorValid = { errors => BadRequest(errors.toString) },
        errorOp = { errors => BadRequest(errors.toString) },
        success = { value => Ok(Json.toJson(value)) }
      ))
    }
  } 
}

