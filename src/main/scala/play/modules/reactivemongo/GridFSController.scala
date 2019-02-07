/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package play.modules.reactivemongo

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.{HttpChunk, HttpEntity}

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{BodyParser, BodyParsers, Controller, MultipartFormData, ResponseHeader, Result}
import play.api.libs.json.{JsObject, JsValue, Json, Reads}
import play.api.libs.streams.{Accumulator, Streams}
import play.core.parsers.Multipart
import reactivemongo.api.gridfs.{DefaultFileToSave, FileToSave, GridFS, ReadFile}
import reactivemongo.play.json._

/** A JSON implementation of `FileToSave`. */
class JSONFileToSave(
  val filename: Option[String]    = None,
  val contentType: Option[String] = None,
  val uploadDate: Option[Long]    = None,
  val metadata: JsObject          = Json.obj(),
  val id: JsValue                 = Json.toJson(UUID.randomUUID().toString))
    extends FileToSave[JSONSerializationPack.type, JsValue] {
  val pack = JSONSerializationPack
}

/** Factory of [[JSONFileToSave]]. */
object JSONFileToSave {
  def apply[N](
    filename: N,
    contentType: Option[String] = None,
    uploadDate: Option[Long]    = None,
    metadata: JsObject          = Json.obj(),
    id: JsValue                 = Json.toJson(UUID.randomUUID().toString))(
    implicit naming: DefaultFileToSave.FileName[N]): JSONFileToSave =
    new JSONFileToSave(naming(filename), contentType, uploadDate, metadata, id)

}

object GridFSController {

  import play.api.libs.json.{JsError, JsResult, JsSuccess}
  import play.api.libs.functional.syntax._
  import reactivemongo.play.json.BSONFormats, BSONFormats.{BSONDateTimeFormat, BSONDocumentFormat}

  implicit def readFileReads[Id <: JsValue](implicit r: Reads[Id]): Reads[ReadFile[JSONSerializationPack.type, Id]] =
    new Reads[ReadFile[JSONSerializationPack.type, Id]] {
      def reads(json: JsValue): JsResult[JsReadFile[Id]] = json match {
        case obj: JsObject =>
          for {
            doc <- BSONDocumentFormat.partialReads(obj)
            _id <- (obj \ "_id").validate[Id]
            ct  <- readOpt[String](obj \ "contentType")
            fn <- (obj \ "filename").toOption.fold[JsResult[Option[String]]](
                   JsSuccess(Option.empty[String])
                 ) { jsVal =>
                   BSONStringFormat.partialReads(jsVal).map(s => Some(s.value))
                 }
            ud <- (obj \ "uploadDate").toOption.fold[JsResult[Option[Long]]](
                   JsSuccess(Option.empty[Long])
                 ) { jsVal =>
                   BSONDateTimeFormat.partialReads(jsVal).map(d => Some(d.value))
                 }
            ck  <- (obj \ "chunkSize").validate[Int]
            len <- (obj \ "length").validate[Long]
            m5  <- readOpt[String](obj \ "md5")
            mt  <- readOpt[JsObject](obj \ "metadata")
          } yield
            new JsReadFile[Id] {
              val pack        = JSONSerializationPack
              val id          = _id
              val contentType = ct
              val filename    = fn
              val uploadDate  = ud
              val chunkSize   = ck
              val length      = len
              val md5         = m5
              val metadata    = mt.getOrElse(Json.obj())
              val original    = doc
            }

        case js => JsError(s"object is expected: $js")
      }
    }

  /*$ GridFS using the JSON serialization pack. */
  type JsGridFS = GridFS[JSONSerializationPack.type]

  type JsFileToSave[T]       = FileToSave[JSONSerializationPack.type, T]
  type JsReadFile[T]         = ReadFile[JSONSerializationPack.type, T]
  type JsGridFSBodyParser[T] = BodyParser[MultipartFormData[JsReadFile[T]]]
}

//TODO: consider if https://github.com/sgodbillon/reactivemongo-demo-app/blob/master/app/controllers/Articles.scala saveAttachment(), getAttachment(), & removeAttachment() functions can be integrated
// and if so make sure appropriate credit / licensing is given to Reactivemongo
trait GridFSController {
  self: Controller =>

  import reactivemongo.api.Cursor
  import GridFSController._

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE     = "inline"

  /**
    * Returns a future Result that serves the first matched file,
    * or [[play.api.mvc.Results.NotFound]].
    */
  def serve[Id <: JsValue, T <: JsReadFile[Id]](gfs: JsGridFS)(
    foundFile: Cursor[T],
    dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit ec: ExecutionContext): Future[Result] =
    foundFile.headOption
      .filter(_.isDefined)
      .map(_.get)
      .map { file =>
        val filename       = file.filename.getOrElse("file.bin")
        @inline def gfsPub = Streams.enumeratorToPublisher(gfs.enumerate(file))
        @inline def chunks = Source.fromPublisher(gfsPub).map(bytes => HttpChunk.Chunk(ByteString.fromArray(bytes)))
        val contentType    = file.contentType.getOrElse("application/octet-stream")
        @inline def gfsEnt = HttpEntity.Chunked(chunks, Some(contentType))

        Result(
          header = ResponseHeader(OK),
          body   = gfsEnt
        ).as(contentType)
          .withHeaders(
            CONTENT_LENGTH -> file.length.toString,
            CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="$filename"; filename*=UTF-8''""" + java.net.URLEncoder
              .encode(filename, "UTF-8")
              .replace("+", "%20"))
          )

      }
      .recover {
        case _ => NotFound
      }

  def gridFSBodyParser[Id <: JsValue](gfs: JsGridFS, fileToSave: (String, Option[String]) => JsFileToSave[Id])(
    implicit readFileReader: Reads[JsReadFile[Id]],
    ec: ExecutionContext,
    materialize: Materializer,
    ir: Reads[Id]): BodyParser[MultipartFormData[Future[JsReadFile[Id]]]] =
    parse.multipartFormData {
      case Multipart.FileInfo(partName, filename, contentType) =>
        val gfsIt = gfs.iteratee(fileToSave(filename, contentType))
        val sink  = Streams.iterateeToAccumulator(gfsIt).toSink
        Accumulator(
          sink.contramap[ByteString](_.toArray[Byte])
        ).map { ref =>
          MultipartFormData.FilePart(partName, filename, contentType, ref)
        }
    }

  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(
    implicit readFileReader: Reads[JsReadFile[Id]],
    materializer: Materializer,
    ir: Reads[Id]): JsGridFSBodyParser[Id] = {
    implicit def ec: ExecutionContext = materializer.executionContext

    parse.multipartFormData {
      case Multipart.FileInfo(partName, filename, contentType) =>
        Accumulator.flatten(gfs.map { gridFS =>
          val gfsIt = gridFS.iteratee(fileToSave(filename, contentType))
          val sink  = Streams.iterateeToAccumulator(gfsIt).toSink

          Accumulator(
            sink.contramap[ByteString](_.toArray[Byte])
          ).mapFuture {
            _.map { ref =>
              MultipartFormData.FilePart(partName, filename, contentType, ref)
            }
          }
        })
    }
  }
}
