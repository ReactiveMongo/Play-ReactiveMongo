/*
 * Copyright 2012 Stephane Godbillon
 * @sgodbillon
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
import reactivemongo.bson.handlers._
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.{Future, ExecutionContext}

/**
 * A mixin for controllers that will provide MongoDB actions.
 */
trait MongoController {
  self :Controller =>

  implicit def connection = ReactiveMongoPlugin.connection
  implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

  /**
   * Returns a future Result that serves the first matched file, or NotFound.
   */
  def serve[T <: ReadFile[_ <: BSONValue]](gfs: GridFS, foundFile: Cursor[T])(implicit ec: ExecutionContext) :Future[Result] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      val en = gfs.enumerate(file)
      SimpleResult(
        // prepare the header
        header = ResponseHeader(200, Map(
            CONTENT_LENGTH -> ("" + file.length),
            CONTENT_DISPOSITION -> ("attachment; filename=\"" + file.filename + "\"; filename*=UTF-8''" + java.net.URLEncoder.encode(file.filename, "UTF-8").replace("+", "%20")),
            CONTENT_TYPE -> file.contentType.getOrElse("application/octet-stream")
        )),
        // give Play this file enumerator
        body = en
      )
    }.recover {
      case _ => NotFound
    }
  }

  /**
   * Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store.
   */
  def gridFSBodyParser(gfs: GridFS)(implicit ec: ExecutionContext) :BodyParser[MultipartFormData[Future[ReadFile[BSONValue]]]] = {
    import BodyParsers.parse._
    import reactivemongo.api.gridfs.Implicits._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(DefaultFileToSave(filename, contentType))
    })
  }

  /**
   * Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store.
   */
  def gridFSBodyParser[Id <: BSONValue](gfs: GridFS, fileToSave: (String, Option[String]) => FileToSave[Id])(implicit readFileReader: BSONReader[ReadFile[Id]], ec: ExecutionContext) :BodyParser[MultipartFormData[Future[ReadFile[Id]]]] = {
    import BodyParsers.parse._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(fileToSave(filename, contentType))
    })
  }
}