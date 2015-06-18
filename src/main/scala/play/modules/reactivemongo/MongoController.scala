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

import reactivemongo.api._
import reactivemongo.api.gridfs._
import reactivemongo.bson._
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.{ Future, ExecutionContext }
import play.modules.reactivemongo.json.JSONSerializationPack

object MongoController {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import play.modules.reactivemongo.json.BSONFormats, BSONFormats.BSONDocumentFormat

  implicit def readFileReads[Id <: BSONValue](implicit r: Reads[Id]): Reads[ReadFile[Id]] = new Reads[ReadFile[Id]] {
    def reads(json: JsValue): JsResult[ReadFile[Id]] = json match {
      case obj: JsObject => for {
        doc <- BSONDocumentFormat.partialReads(obj)
        _id <- (obj \ "_id").validate[Id]
        ct <- (obj \ "contentType").validate[Option[String]]
        fn <- (obj \ "filename").validate[String]
        ud <- (obj \ "uploadDate").validate[Option[Long]]
        ck <- (obj \ "chunkSize").validate[Int]
        len <- (obj \ "length").validate[Int]
        m5 <- (obj \ "md5").validate[Option[String]]
        mt <- (obj \ "metadata").validate[JsObject].flatMap(
          o => BSONDocumentFormat.partialReads(o))
      } yield new ReadFile[Id] {
        def id = _id
        val contentType = ct
        val filename = fn
        val uploadDate = ud
        val chunkSize = ck
        val length = len
        val md5 = m5
        val metadata = mt
        val original = doc
      }

      case js => JsError(s"object is expected: $js")
    }
  }
}

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController {
  self: Controller =>

  import play.api.libs.json.Reads
  import MongoController._

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE = "inline"

  /** Returns a future Result that serves the first matched file, or NotFound. */
  def serve[T <: ReadFile[_ <: BSONValue], Structure, Reader[_], Writer[_]](gfs: GridFS[JSONSerializationPack.type], foundFile: Cursor[T], dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit ec: ExecutionContext): Future[Result] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      val filename = file.filename

      Result(header = ResponseHeader(OK), body = gfs.enumerate(file)).
        as(file.contentType.getOrElse("application/octet-stream")).
        withHeaders(CONTENT_LENGTH -> file.length.toString, CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*=UTF-8''""" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")))

    }.recover {
      case _ => NotFound
    }
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser[Reader[_]](gfs: GridFS[JSONSerializationPack.type])(implicit readFileReader: Reader[ReadFile[BSONValue]], ec: ExecutionContext): BodyParser[MultipartFormData[Future[ReadFile[BSONValue]]]] = {
    import BodyParsers.parse._
    implicit val bsonReads =
      play.modules.reactivemongo.json.BSONFormats.BSONValueReads

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(DefaultFileToSave(filename, contentType))
    })
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser[Reader[_], Id <: BSONValue](gfs: GridFS[JSONSerializationPack.type], fileToSave: (String, Option[String]) => FileToSave[Id])(implicit readFileReader: Reader[ReadFile[Id]], ec: ExecutionContext, ir: Reads[Id]): BodyParser[MultipartFormData[Future[ReadFile[Id]]]] = {
    import BodyParsers.parse._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(fileToSave(filename, contentType))
    })
  }
}
