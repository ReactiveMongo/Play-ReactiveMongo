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

/** A mixin for controllers that will provide MongoDB actions. */
trait MongoController {
  self: Controller =>

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  val CONTENT_DISPOSITION_ATTACHMENT = "attachment"
  val CONTENT_DISPOSITION_INLINE = "inline"

  /** Returns a future Result that serves the first matched file, or NotFound. */
  def serve[T <: ReadFile[_ <: BSONValue], Structure, Reader[_], Writer[_]](gfs: GridFS[Structure, Reader, Writer], foundFile: Cursor[T], dispositionMode: String => String)(implicit ec: ExecutionContext): Future[SimpleResult] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      val en = gfs.enumerate(file)
      val filename = file.filename
      val contentType = file.contentType.getOrElse("application/octet-stream")
      SimpleResult(
        // prepare the header
        header = ResponseHeader(OK, Map(
          CONTENT_LENGTH -> ("" + file.length),
          CONTENT_DISPOSITION -> (s"""${dispositionMode(contentType)}; filename="$filename"; filename*=UTF-8''""" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")),
          CONTENT_TYPE -> contentType)),
        // give Play this file enumerator
        body = en)
    }.recover {
      case _ => NotFound
    }
  }

  def serve[T <: ReadFile[_ <: BSONValue], Structure, Reader[_], Writer[_]](gfs: GridFS[Structure, Reader, Writer], foundFile: Cursor[T], dispositionMode: String = CONTENT_DISPOSITION_ATTACHMENT)(implicit ec: ExecutionContext): Future[SimpleResult] = serve(gfs, foundFile, { _ => dispositionMode })(ec)

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser[Structure, Reader[_], Writer[_]](gfs: GridFS[Structure, Reader, Writer])(implicit readFileReader: Reader[ReadFile[BSONValue]], ec: ExecutionContext): BodyParser[MultipartFormData[Future[ReadFile[BSONValue]]]] = {
    import BodyParsers.parse._
    import reactivemongo.api.gridfs.Implicits._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(DefaultFileToSave(filename, contentType))
    })
  }

  /** Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store. */
  def gridFSBodyParser[Structure, Reader[_], Writer[_], Id <: BSONValue](gfs: GridFS[Structure, Reader, Writer], fileToSave: (String, Option[String]) => FileToSave[Id])(implicit readFileReader: Reader[ReadFile[Id]], ec: ExecutionContext): BodyParser[MultipartFormData[Future[ReadFile[Id]]]] = {
    import BodyParsers.parse._

    multipartFormData(Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        gfs.iteratee(fileToSave(filename, contentType))
    })
  }
}
