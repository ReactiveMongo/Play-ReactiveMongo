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
package play.modules.mongodb

import org.asyncmongo.api._
import org.asyncmongo.api.gridfs._
import play.api.libs.iteratee._
import play.api.mvc._
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.util.{Duration, DurationInt}

/**
 * A mixin for controllers that will provide MongoDB actions.
 */
trait MongoController {
  self :Controller =>

  /** wait for primary timeout. */
  val timeout :Duration = new DurationInt(1).seconds

  /**
   * A helper that checks that a primary is there before executing the given function and returns an AsyncResult.
   */
  def MongoAsyncResult(whenReady: => Future[Result])(implicit connection: MongoConnection, ec: ExecutionContext) :AsyncResult =
    Async {
      connection.waitForPrimary(timeout).flatMap(e => whenReady)
    }

  /**
   * Returns a future Result that serves the first matched file, or NotFound.
   */
  def serve(foundFile: Cursor[ReadFileEntry])(implicit ec: ExecutionContext) :Future[Result] = {
    foundFile.headOption.filter(_.isDefined).map(_.get).map { file =>
      SimpleResult(
        // prepare the header
        header = ResponseHeader(200, Map(
            CONTENT_LENGTH -> ("" + file.length),
            CONTENT_DISPOSITION -> ("attachment; filename=\"" + file.filename + "\"; filename*=UTF-8''" + java.net.URLEncoder.encode(file.filename, "UTF-8").replace("+", "%20")),
            CONTENT_TYPE -> file.contentType.getOrElse("application/octet-stream")
        )),
        // give Play this file enumerator
        body = file.enumerate
      )
    }.recover {
      case _ => NotFound
    }
  }

  /**
   * Gets a body parser that will save a file sent with multipart/form-data into the given GridFS store.
   */
  def gridFSBodyParser(gfs: GridFS)(implicit ec: ExecutionContext) :BodyParser[MultipartFormData[Future[PutResult]]] = {
    import BodyParsers.parse._

    multipartFormData(Multipart.handleFilePart {
      case fileinfo @ Multipart.FileInfo(partName, filename, contentType) =>
        val iteratee = Iteratee.flatten(gfs.db.connection.waitForPrimary(timeout).map { _ =>
          gfs.save(filename, None, contentType)
        })
        iteratee
    })
  }
}