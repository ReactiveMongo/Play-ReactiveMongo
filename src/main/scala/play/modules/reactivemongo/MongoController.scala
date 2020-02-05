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

import scala.concurrent.{ Future, ExecutionContext }

import akka.stream.Materializer

import play.api.mvc.{
  BodyParser,
  MultipartFormData,
  Result,
  ResponseHeader
}
import play.api.http.{ HttpChunk, HttpEntity }
import play.api.libs.json.{ JsObject, JsValue }
import play.api.libs.streams.Accumulator

import reactivemongo.api.gridfs.{ FileToSave, GridFS, ReadFile }

import reactivemongo.play.json._

object MongoController {
  /** GridFS using the JSON serialization pack. */
  type JsGridFS = GridFS[JSONSerializationPack.type]

  type JsFileToSave[T] = FileToSave[T, JsObject]
  type JsReadFile[T] = ReadFile[T, JsObject]
  type JsGridFSBodyParser[T] = BodyParser[MultipartFormData[JsReadFile[T]]]
}

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController extends PlaySupport.Controller {
  self: ReactiveMongoComponents =>

  import reactivemongo.api.Cursor
  import reactivemongo.akkastream.GridFSStreams
  import MongoController._

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

  def gridFSBodyParser(gfs: Future[JsGridFS])(implicit materializer: Materializer): JsGridFSBodyParser[JsValue] = parser(gfs, { (g, n, t) => g.fileToSave(Some(n), t) })(materializer)

  private def parser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (JsGridFS, String, Option[String]) => JsFileToSave[Id])(implicit materializer: Materializer): JsGridFSBodyParser[Id] = {
    implicit def ec: ExecutionContext = materializer.executionContext

    parse.multipartFormData {
      case PlaySupport.FileInfo(partName, filename, contentType) =>
        Accumulator.flatten(gfs.map { gridFS =>
          val fileRef = fileToSave(gridFS, filename, contentType)
          val sink = GridFSStreams(gridFS).sinkWithMD5(fileRef)

          Accumulator(sink).map { ref =>
            MultipartFormData.FilePart(partName, filename, contentType, ref)
          }
        })
    }
  }
}
