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

import scala.concurrent.{ ExecutionContext, Future }

import play.api.http.{ HttpChunk, HttpEntity }
import play.api.mvc.{ BodyParser, MultipartFormData, ResponseHeader, Result }

import reactivemongo.api.bson.{ BSONDocument, BSONValue }
import reactivemongo.api.bson.collection.BSONSerializationPack

import org.apache.pekko.stream.Materializer

object MongoController {
  type GridFS = reactivemongo.api.gridfs.GridFS[BSONSerializationPack.type]

  type GridFSBodyParser[T <: BSONValue] = BodyParser[
    MultipartFormData[reactivemongo.api.gridfs.ReadFile[T, BSONDocument]]
  ]

  type FileToSave[T <: BSONValue] =
    reactivemongo.api.gridfs.FileToSave[T, BSONDocument]

  /** `Content-Disposition: attachment` */
  private[reactivemongo] val CONTENT_DISPOSITION_ATTACHMENT = "attachment"

  /** `Content-Disposition: inline` */
  private[reactivemongo] val CONTENT_DISPOSITION_INLINE = "inline"

}

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController extends PlaySupport.Controller {
  self: ReactiveMongoComponents =>

  import reactivemongo.api.Cursor
  import reactivemongo.pekkostream.GridFSStreams
  import MongoController._

  /**
   * Returns the current MongoConnection instance
   * (the connection pool manager).
   */
  protected final def connection = reactiveMongoApi.connection

  /** Returns the default database (as specified in `application.conf`). */
  protected final def database = reactiveMongoApi.database

  /**
   * Returns a future Result that serves the first matched file,
   * or a `NotFound` result.
   */
  protected final def serve[Id <: BSONValue](
      gfs: GridFS
    )(foundFile: Cursor[gfs.ReadFile[Id]],
      dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT
    )(implicit
      materializer: Materializer
    ): Future[Result] = {
    implicit def ec: ExecutionContext = materializer.executionContext

    foundFile.headOption.collect { case Some(file) => file }.map { file =>
      def filename = file.filename.getOrElse("file.bin")
      def contentType = file.contentType.getOrElse("application/octet-stream")

      def chunks = GridFSStreams(gfs).source(file).map(HttpChunk.Chunk(_))

      Result(
        header = ResponseHeader(OK),
        body = HttpEntity.Chunked(chunks, Some(contentType))
      ).as(contentType)
        .withHeaders(
          CONTENT_LENGTH -> file.length.toString,
          CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*="UTF-8''""" + java.net.URLEncoder
            .encode(filename, "UTF-8")
            .replace("+", "%20") + '"')
        )

    }.recover { case _ => NotFound }
  }

  protected final def gridFSBodyParser(
      gfs: Future[GridFS]
    )(implicit
      materializer: Materializer
    ): GridFSBodyParser[BSONValue] = {
    implicit def ec: ExecutionContext = materializer.executionContext
    import play.api.libs.streams.Accumulator

    parse.multipartFormData { (in: Any) =>
      in match {
        case PlaySupport.FileInfo(partName, filename, contentType) =>
          Accumulator.flatten(gfs.map { gridFS =>
            val fileRef = gridFS.fileToSave( // see Api.scala
              filename = Some(filename),
              contentType = contentType
            )

            val sink = GridFSStreams(gridFS).sinkWithMD5(fileRef)

            Accumulator(sink).map { ref =>
              MultipartFormData.FilePart(partName, filename, contentType, ref)
            }
          })

        case info =>
          sys.error(s"Unsupported: $info")
      }
    }
  }
}
