package controllers

import play.api._
import play.api.mvc._

import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.data.validation.Constraints._
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.util._

//import play.api.libs.json.Constraints._
import play.api.data.validation.ValidationError
import play.api.cache.Cache
import scala.concurrent.util.duration._
import scala.concurrent.util.Duration
//import akka.dispatch.Future


import play.api.libs.concurrent.execution.defaultContext

object Application extends Controller {
  val timeout :Duration = 1 seconds
  implicit val connection = ReactiveMongoPlugin.connection

  lazy val coll = ReactiveMongoPlugin.collection("castors")  
  val d = new java.util.Date()
  
  def index = Action { implicit request =>
    Ok(views.html.index("castors")) 
  }
  
  lazy val mongoEnum = coll.find[JsValue](
    QueryBuilder(), QueryOpts().tailable.awaitData
  ).enumerate()

  lazy val out = Concurrent.broadcast(mongoEnum)._1

  import Constraints._

  val ReadsFromWeb = (
    (__ \ "type" ).read[String] and
    (__ \ "data" ).read(
      verifyingIf[JsObject]{ case JsObject(fields) => !fields.isEmpty }(
        (__ \ "uuid").read[String] and 
        (__ \ "title").read[String] and 
        (__ \ "created").read[java.util.Date] tupled
      )
    )
  ) tupled

  val WritesToMongo = __.json.modify(
    (__ \ "created").json.transform( js => Json.obj("$date" -> js) )
  )

  val WritesToWeb = (
    (__ \ "type").json.write(JsString("castor")) and 
    (__ \ "data").json.create(
      __.json.modify ( 
        (__ \ "created").json.transform( js => js \ "$date" )
      )
    ) 
  ) flattened

  def watchCastors = WebSocket.async[JsValue] { implicit request => 
    connection.waitForPrimary(timeout).map { _ =>
      // generates random UUID + stores it in cache
      val uuid = java.util.UUID.randomUUID().toString()
      Cache.set("u$"+uuid, List[String]())

      // builds Input iteratee that validates JS
      val in = Iteratee.foreach[JsValue] { json =>
        json.validate(ReadsFromWeb).fold(
          valid = { case (typ, data) => 
            play.Logger.info("received:%s %s".format(typ, data))
            typ match {
              case "castor" => coll.insert( data.transform(WritesToMongo) )                
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
          out through
            // filters same UUID 
            Enumeratee.filter{ js => 
              (js \ "uuid").asOpt[String].map( _ != uuid ).getOrElse(true)
            } through
            // filters title with filters
            Enumeratee.filter{ js => 
              (js \ "title").asOpt[String]
                .flatMap{ title =>
                  Cache.getAs[List[String]]("u$"+uuid).map{ filters =>
                    filters.isEmpty || !filters.exists(title.toLowerCase.contains(_))
                  }
                }.getOrElse(true)
            } through
            // transforms to send out
            Enumeratee.map{ js =>  
              val s = js.transform(WritesToWeb)
              play.Logger.info("sending %s".format(s))
              s
            }
        )

        ( in, filterOut )
      }
    
  }

  val filterFmt = (__ \ "filters").read[List[String]]

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

/*def mongoDateFormat: Format[java.util.Date] = new Format[java.util.Date] {    
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

  }*/

}
