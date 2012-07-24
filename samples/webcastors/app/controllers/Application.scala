package controllers

import play.api._
import play.api.mvc._

import play.modules.mongodb._
import play.modules.mongodb.PlayBsonImplicits._

import org.asyncmongo.api._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import org.asyncmongo.bson._
import org.asyncmongo.protocol._

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.data.validation.Constraints._
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Constraints._
import play.api.data.validation.ValidationError
import play.api.cache.Cache

import akka.dispatch.Future

object Application extends Controller {
  val coll = MongoAsyncPlugin.collection("castors")  
  val d = new java.util.Date()
  
  def index = Action { implicit request =>
    Ok(views.html.index("castors"))
  }
  
  val out = Concurrent.broadcast(Cursor.enumerate(
    coll.find[JsValue, JsValue, JsValue](Json.obj(), None, 0, 0, QueryFlags.TailableCursor | QueryFlags.AwaitData)
  ), () )._1


  // a custom reads that can accept an empty JsObject
  object canBeEmptyJsObject extends Reads[JsObject] {
    def reads(json: JsValue) = json match {
      case js @ JsObject(fields) => JsSuccess(js)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsobject"))))
    }
  }

  implicit def customDate: Format[java.util.Date] = new Format[java.util.Date] {    
    def reads(json: JsValue): JsResult[java.util.Date] = (json \ "$date").asOpt[JsValue] match {
      case Some(JsNumber(d)) => JsSuccess(new java.util.Date(d.toInt))
      case Some(JsString(s)) => parseDate(s) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date.isoformat"))))
      }

      case None | Some(_) => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }

    def writes(d: java.util.Date) = Json.obj("$date" -> JsNumber(d.getTime))


    def parseDate(input: String): Option[java.util.Date] = {
      //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
      //things a bit.  Before we go on we have to repair this.
      val df = new java.text.SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" )
      
      //this is zero time so we need to add that TZ indicator for 
      val inputStr = if ( input.endsWith( "Z" ) ) {
          input.substring( 0, input.length() - 1) + "GMT-00:00"
        } else {
            val inset = 6
        
            val s0 = input.substring( 0, input.length - inset )
            val s1 = input.substring( input.length - inset, input.length )

            s0 + "GMT" + s1
        }
      
      try { Some(df.parse( input )) } catch {
        case _: java.text.ParseException => None
      }
      
    }

    def toString(date: java.util.Date): String = {
        val df = new java.text.SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" )
        val tz = java.util.TimeZone.getTimeZone( "UTC" )
        df.setTimeZone( tz );

        val output = df.format( date )
        val inset0 = 9
        val inset1 = 6
        
        val s0 = output.substring( 0, output.length - inset0 )
        val s1 = output.substring( output.length - inset1, output.length )

        val result = s0 + s1

        result.replaceAll( "UTC", "+00:00" )
    }

  }

  implicit val castorScan = JsTupler(
    JsPath \ "type" -> in(required[String]),
    JsPath \ "data" -> in(
      canBeEmptyJsObject or 
      (
        JsTupler(
          JsPath \ "uuid" -> in(required[String]),
          JsPath \ "title" -> in(required[String]),
          JsPath \ "created" -> in(required[java.util.Date])
        ) 
        andThen of[JsObject]
      )
    )
  )

  def watchCastors = WebSocket.using[JsValue] { implicit request => 
    // generates random UUID + stores it in cache
    val uuid = java.util.UUID.randomUUID().toString()
    Cache.set("u$"+uuid, List[String]())

    // builds Input iteratee that validates JS
    val in = Iteratee.foreach[JsValue] { json =>
      json.validate(castorScan).fold(
        valid = { case (typ, data) => 
          typ match {
            case "castor" => coll.insert(data)
            case "connect" => // do nothing
          } 
        },
        invalid = e => println("error:%s".format(e))
      )
    }

    val filterOut = 
      // sends first a connect msg with uuid
      Enumerator(Json.obj("type" -> "connect", "uuid" -> uuid).as[JsValue]) >>> 
      ( 
        // now gets castors from Mongo and filter them according to filters and not same UUID
        out &> Enumeratee.filter{ js => 
          (js \ "uuid").asOpt[String].exists{ recuuid => 
            if(recuuid == uuid) {
              println("same uuid...skipping")
              false
            } else {
              (js \ "title").asOpt[String].exists{ t => 
                val filters = Cache.getAs[List[String]]("u$"+uuid)
                filters.exists{ filters => 
                  filters.isEmpty || filters.exists( t.contains(_) )
                }
              }
            }
          }
        } &> Enumeratee.map{ js => 
          val s = Json.obj("type" -> "castor", "data" -> js)
          play.Logger.info("sending %s".format(s))
          s
        }
      )

    ( in, filterOut )
  }

  implicit val filterFmt = JsTupler(
    JsPath \ "filters" -> in(required[List[String]])
  )

  def getFilters(uuid: String) = Action { request =>
    Cache.getAs[List[String]]("u$"+uuid)
      .map( filters => Ok(Json.obj( "uuid" -> uuid, "filters" -> Json.toJson(filters))) )
      .getOrElse(BadRequest("not found in cache"))
  }

  def addFilters(uuid: String) = Action(parse.json) { request =>
    val js = request.body

    js.validate(filterFmt).fold(
      valid = { filters =>
        val cid = "u$"+uuid
        Cache.getAs[List[String]](cid).map{ f => Cache.set(cid, f ++ filters) }
        Ok("Filters %s Added".format(filters))
      },
      invalid = e => BadRequest(e.toString)
    )
    
  }

  def removeFilters(uuid: String) = Action(parse.json) { request =>
    val js = request.body

    js.validate(filterFmt).fold(
      valid = { filters =>
        val cid = "u$"+uuid
        Cache.getAs[List[String]](cid).map{ f => Cache.set(cid, f.filterNot( filters contains _ ) ) }
        Ok("Filters %s Removed".format(filters))
      },
      invalid = e => BadRequest(e.toString)
    )
    
  }


  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        controllers.routes.javascript.Application.addFilters,
        controllers.routes.javascript.Application.removeFilters
      )
    ).as("text/javascript") 
  }

}
