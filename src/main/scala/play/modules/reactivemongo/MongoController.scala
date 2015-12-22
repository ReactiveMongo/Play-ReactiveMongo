/*
 * Copyright 2012-2013 Stephane Godbillon (@sgodbillon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.modules.reactivemongo

import java.util.UUID

import scala.concurrent.{ Future, ExecutionContext }

import play.api.mvc.{
  Action,
  BodyParser,
  BodyParsers,
  Controller,
  MultipartFormData,
  Result,
  ResponseHeader
}
import play.api.Play.current
import play.api.libs.json.{ Json, JsObject, JsString, JsValue, Reads }

import reactivemongo.api.gridfs.{
  DefaultFileToSave,
  FileToSave,
  GridFS,
  ReadFile
}

import reactivemongo.play.json._

/** A JSON implementation of `FileToSave`. */
class JSONFileToSave(
  val filename: Option[String] = None,
  val contentType: Option[String] = None,
  val uploadDate: Option[Long] = None,
  val metadata: JsObject = Json.obj(),
  val id: JsValue = Json.toJson(UUID.randomUUID().toString))
    extends FileToSave[JSONSerializationPack.type, JsValue] {
  val pack = JSONSerializationPack
}

/** Factory of [[JSONFileToSave]]. */
object JSONFileToSave {
  def apply[N](filename: N,
               contentType: Option[String] = None,
               uploadDate: Option[Long] = None,
               metadata: JsObject = Json.obj(),
               id: JsValue = Json.toJson(UUID.randomUUID().toString))(implicit naming: DefaultFileToSave.FileName[N]): JSONFileToSave = new JSONFileToSave(naming(filename), contentType, uploadDate, metadata, id)

}

object MongoController {
  import reactivemongo.bson.BSONDateTime
  import play.api.libs.json.{ JsError, JsResult, JsSuccess }
  import reactivemongo.play.json.BSONFormats, BSONFormats.{ BSONDateTimeFormat, BSONDocumentFormat }

  implicit def readFileReads[Id <: JsValue](implicit r: Reads[Id]): Reads[ReadFile[JSONSerializationPack.type, Id]] = new Reads[ReadFile[JSONSerializationPack.type, Id]] {
    def reads(json: JsValue): JsResult[ReadFile[JSONSerializationPack.type, Id]] = json match {
      case obj: JsObject => for {
        doc <- BSONDocumentFormat.partialReads(obj)
        _id <- (obj \ "_id").validate[Id]
        ct <- (obj \ "contentType").validate[Option[String]]
        fn <- (obj \ "filename").validate[Option[String]]
        ud <- (obj \ "uploadDate").validate[Option[JsObject]].flatMap {
          case Some(obj) =>
            BSONDateTimeFormat.partialReads(obj).map(d => Some(d.value))

          case _ => JsSuccess(Option.empty[Long])
        }
        ck <- (obj \ "chunkSize").validate[Int]
        len <- (obj \ "length").validate[Long]
        m5 <- (obj \ "md5").validate[Option[String]]
        mt <- (obj \ "metadata").validate[Option[JsObject]]
      } yield new ReadFile[JSONSerializationPack.type, Id] {
        val pack = JSONSerializationPack
        val id = _id
        val contentType = ct
        val filename = fn
        val uploadDate = ud
        val chunkSize = ck
        val length = len
        val md5 = m5
        val metadata = mt.getOrElse(Json.obj())
        val original = doc
      }

      case js => JsError(s"object is expected: $js")
    }
  }
}

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController { self: Controller =>

  import reactivemongo.api.Cursor

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver

  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection

  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE = "inline"

  /**
   * Returns a future Result that serves the first matched file, or NotFound.
   */
  def serve[Id <: JsValue, T <: ReadFile[JSONSerializationPack.type, Id]](gfs: GridFS[JSONSerializationPack.type])(foundFile: Cursor[T], dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit ec: ExecutionContext): Future[Result] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      val filename = file.filename.getOrElse("file.bin")

      Result(header = ResponseHeader(OK), body = gfs.enumerate(file)).
        as(file.contentType.getOrElse("application/octet-stream")).
        withHeaders(CONTENT_LENGTH -> file.length.toString, CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*=UTF-8''""" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")))

    }.recover {
      case _ => NotFound
    }
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser(gfs: GridFS[JSONSerializationPack.type])(implicit readFileReader: Reads[ReadFile[JSONSerializationPack.type, JsValue]], ec: ExecutionContext): BodyParser[MultipartFormData[Future[ReadFile[JSONSerializationPack.type, JsValue]]]] = {
    import BodyParsers.parse._
    implicit val bsonReads =
      play.modules.reactivemongo.json.BSONValueReads

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(JSONFileToSave(Some(filename), contentType))
    })
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser[Id <: JsValue](gfs: GridFS[JSONSerializationPack.type], fileToSave: (String, Option[String]) => FileToSave[JSONSerializationPack.type, Id])(implicit readFileReader: Reads[ReadFile[JSONSerializationPack.type, Id]], ec: ExecutionContext, ir: Reads[Id]): BodyParser[MultipartFormData[Future[ReadFile[JSONSerializationPack.type, Id]]]] = {
    import BodyParsers.parse._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(fileToSave(filename, contentType))
    })
  }
}
