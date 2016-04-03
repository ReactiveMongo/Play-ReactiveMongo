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

import akka.util.ByteString
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }

import play.api.mvc.{
  Action,
  BodyParser,
  BodyParsers,
  Controller,
  MultipartFormData,
  Result,
  ResponseHeader
}
import play.api.http.{ HttpChunk, HttpEntity }
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{ Json, JsObject, JsString, JsValue, Reads }
import play.api.libs.streams.{ Accumulator, Streams }

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
        ct <- readOpt[String](obj \ "contentType")
        fn <- (obj \ "filename").toOption.fold[JsResult[Option[String]]](
          JsSuccess(Option.empty[String])) { jsVal =>
            BSONStringFormat.partialReads(jsVal).map(s => Some(s.value))
          }
        ud <- (obj \ "uploadDate").toOption.fold[JsResult[Option[Long]]](
          JsSuccess(Option.empty[Long])) { jsVal =>
            BSONDateTimeFormat.partialReads(jsVal).map(d => Some(d.value))
          }
        ck <- (obj \ "chunkSize").validate[Int]
        len <- (obj \ "length").validate[Long]
        m5 <- readOpt[String](obj \ "md5")
        mt <- readOpt[JsObject](obj \ "metadata")
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
trait MongoController extends Controller { self: ReactiveMongoComponents =>

  import play.core.parsers.Multipart
  import reactivemongo.api.Cursor

  /** Returns the current instance of the driver. */
  def driver = reactiveMongoApi.driver

  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = reactiveMongoApi.connection

  @deprecated(message = "Use [[database]]", since = "0.12.0")
  def db = reactiveMongoApi.db

  /** Returns the default database (as specified in `application.conf`). */
  def database = reactiveMongoApi.database

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE = "inline"

  /**
   * Returns a future Result that serves the first matched file, or NotFound.
   */
  def serve[Id <: JsValue, T <: ReadFile[JSONSerializationPack.type, Id]](gfs: GridFS[JSONSerializationPack.type])(foundFile: Cursor[T], dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit ec: ExecutionContext): Future[Result] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      val filename = file.filename.getOrElse("file.bin")
      @inline def gfsPub = Streams.enumeratorToPublisher(gfs.enumerate(file))
      @inline def chunks = Source.fromPublisher(gfsPub).
        map(bytes => HttpChunk.Chunk(ByteString.fromArray(bytes)))
      val contentType = file.contentType.getOrElse("application/octet-stream")
      @inline def gfsEnt = HttpEntity.Chunked(chunks, Some(contentType))

      Result(
        header = ResponseHeader(OK),
        body = gfsEnt).as(contentType).
        withHeaders(CONTENT_LENGTH -> file.length.toString, CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*=UTF-8''""" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")))

    }.recover {
      case _ => NotFound
    }
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  @deprecated(message = "Use `gridFSBodyParser` with `Future[GridFS]`",
    since = "0.12.0")
  def gridFSBodyParser(gfs: GridFS[JSONSerializationPack.type])(implicit readFileReader: Reads[ReadFile[JSONSerializationPack.type, JsValue]], ec: ExecutionContext, materialize: Materializer): BodyParser[MultipartFormData[Future[ReadFile[JSONSerializationPack.type, JsValue]]]] = gridFSBodyParser(gfs, { (n, t) => JSONFileToSave(Some(n), t) })

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  @deprecated(message = "Use `gridFSBodyParser` with `Future[GridFS]`",
    since = "0.12.0")
  def gridFSBodyParser[Id <: JsValue](gfs: GridFS[JSONSerializationPack.type], fileToSave: (String, Option[String]) => FileToSave[JSONSerializationPack.type, Id])(implicit readFileReader: Reads[ReadFile[JSONSerializationPack.type, Id]], ec: ExecutionContext, materialize: Materializer, ir: Reads[Id]): BodyParser[MultipartFormData[Future[ReadFile[JSONSerializationPack.type, Id]]]] =
    parse.multipartFormData {
      case Multipart.FileInfo(partName, filename, contentType) =>
        val gfsIt = gfs.iteratee(fileToSave(filename, contentType))
        val sink = Streams.iterateeToAccumulator(gfsIt).toSink
        Accumulator(
          sink.contramap[ByteString](_.toArray[Byte])).map { ref =>
            MultipartFormData.FilePart(partName, filename, contentType, ref)
          }
    }
}
