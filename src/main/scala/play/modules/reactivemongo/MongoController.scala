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

import akka.stream.Materializer

import play.api.mvc.{
  BodyParser,
  MultipartFormData,
  Result,
  ResponseHeader
}
import play.api.http.{ HttpChunk, HttpEntity }
import play.api.libs.json.{ Json, JsObject, JsValue, Reads }
import play.api.libs.streams.Accumulator

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
    val id: JsValue = Json.toJson(UUID.randomUUID().toString)) extends FileToSave[JSONSerializationPack.type, JsValue] {
  val pack = JSONSerializationPack
}

/** Factory of [[JSONFileToSave]]. */
object JSONFileToSave {
  def apply[N](
    filename: N,
    contentType: Option[String] = None,
    uploadDate: Option[Long] = None,
    metadata: JsObject = Json.obj(),
    id: JsValue = Json.toJson(UUID.randomUUID().toString))(implicit naming: DefaultFileToSave.FileName[N]): JSONFileToSave = new JSONFileToSave(naming(filename), contentType, uploadDate, metadata, id)

}

object MongoController {
  import play.api.libs.json.{ JsError, JsResult, JsSuccess }
  import reactivemongo.play.json.BSONFormats, BSONFormats.BSONDateTimeFormat

  implicit def readFileReads[Id <: JsValue](implicit r: Reads[Id]): Reads[JsReadFile[Id]] = new Reads[JsReadFile[Id]] {
    def reads(json: JsValue): JsResult[JsReadFile[Id]] = json match {
      case obj: JsObject => for {
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
      } yield new JsReadFile[Id] {
        val pack = JSONSerializationPack
        val id = _id
        val contentType = ct
        val filename = fn
        val uploadDate = ud
        val chunkSize = ck
        val length = len
        val md5 = m5
        val metadata = mt.getOrElse(Json.obj())
      }

      case js => JsError(s"object is expected: $js")
    }
  }

  /** GridFS using the JSON serialization pack. */
  type JsGridFS = GridFS[JSONSerializationPack.type]

  type JsFileToSave[T] = FileToSave[JSONSerializationPack.type, T]
  type JsReadFile[T] = ReadFile[JSONSerializationPack.type, T]
  type JsGridFSBodyParser[T] = BodyParser[MultipartFormData[JsReadFile[T]]]
}

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController extends PlaySupport.Controller {
  self: ReactiveMongoComponents =>

  import reactivemongo.api.Cursor
  import reactivemongo.akkastream.GridFSStreams
  import MongoController._

  /** Returns the current instance of the driver. */
  def driver = reactiveMongoApi.driver

  /**
   * Returns the current MongoConnection instance
   * (the connection pool manager).
   */
  def connection = reactiveMongoApi.connection

  /** Returns the default database (as specified in `application.conf`). */
  def database = reactiveMongoApi.database

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE = "inline"

  /**
   * Returns a future Result that serves the first matched file,
   * or a `NotFound` result.
   */
  def serve[Id <: JsValue, T <: JsReadFile[Id]](gfs: JsGridFS)(foundFile: Cursor[T], dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit materializer: Materializer): Future[Result] = {
    implicit def ec: ExecutionContext = materializer.executionContext

    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      def filename = file.filename.getOrElse("file.bin")
      def contentType = file.contentType.getOrElse("application/octet-stream")

      def chunks = GridFSStreams(gfs).source(file).map(HttpChunk.Chunk(_))

      Result(
        header = ResponseHeader(OK),
        body = HttpEntity.Chunked(chunks, Some(contentType))
      ).as(contentType).
        withHeaders(CONTENT_LENGTH -> file.length.toString, CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*="UTF-8''""" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20") + '"'))

    }.recover {
      case _ => NotFound
    }
  }

  def gridFSBodyParser(gfs: Future[JsGridFS])(implicit @deprecated("Unused", "0.19.0") readFileReader: Reads[JsReadFile[JsValue]] = null.asInstanceOf[Reads[JsReadFile[JsValue]]], materializer: Materializer): JsGridFSBodyParser[JsValue] = parser(gfs, { (n, t) => JSONFileToSave(Some(n), t) })(materializer)

  @deprecated("Use `gridFSBodyParser` without `ir`", "0.17.0")
  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit readFileReader: Reads[JsReadFile[Id]], materializer: Materializer, ir: Reads[Id]): JsGridFSBodyParser[Id] = parser(gfs, fileToSave)

  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit @deprecated("Unused", "0.19.0") readFileReader: Reads[JsReadFile[Id]], materializer: Materializer): JsGridFSBodyParser[Id] = parser(gfs, fileToSave)

  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit materializer: Materializer): JsGridFSBodyParser[Id] = parser(gfs, fileToSave)

  private def parser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit materializer: Materializer): JsGridFSBodyParser[Id] = {
    implicit def ec: ExecutionContext = materializer.executionContext

    parse.multipartFormData {
      case PlaySupport.FileInfo(partName, filename, contentType) =>
        Accumulator.flatten(gfs.map { gridFS =>
          val fileRef = fileToSave(filename, contentType)
          val sink = GridFSStreams(gridFS).sinkWithMD5(fileRef)

          Accumulator(sink).map { ref =>
            MultipartFormData.FilePart(partName, filename, contentType, ref)
          }
        })
    }
  }
}
