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

import reactivemongo.api.Cursor
import play.api.libs.json._
import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.bson.BSONObjectID

import scala.annotation.tailrec

package object extensions {

  implicit class StringExtensions(val string: String) extends AnyVal {

    /**
     * from String to ObjectId
     * @return
     */
    def toBSONObjectID: BSONObjectID = BSONObjectID(string)

    /**
     * Make slug out of string, stripping all unicode characters and replacing spaces with dashes
     * @return a slug
     */
    def slug: String = {
      import java.text.Normalizer
      Normalizer.normalize(string, Normalizer.Form.NFD).trim().replaceAll("[^\\w ]", "").replace(" ", "-").toLowerCase
    }

    /**
     * Make a slug unique, by passing in a seq of existing slugs and appending a number until the slug is unique
     * @param slugs An existing collection of slugs to make unique against
     * @return a unique slug
     */
    def uniqueSlug(slugs: Seq[String]): String = {
      @tailrec
      def makeUnique(slug: String, existingSlugs: Seq[String]): String = {
        if (!(existingSlugs contains slug)) {
          slug
        } else {
          val EndsWithNumber = "(.+-)([0-9]+)$".r
          slug match {
            case EndsWithNumber(s, n) => makeUnique(s + (n.toInt + 1), existingSlugs)
            case s                    => makeUnique(s + "-2", existingSlugs)
          }
        }
      }

      makeUnique(string, slugs)
    }

  }

  implicit class ListExtensions[T](val futureList: Future[List[T]]) extends AnyVal {

    /**
     * Converts Future List of type T to an JsArray.
     * Useful when you want to pipe whatever comes out of reactive mongo directly as json
     *
     * @param ec
     * @param writes
     * @return
     */
    def toJsArray(implicit ec: ExecutionContext, writes: Writes[T]): Future[JsArray] = {
      futureList.map { futureList =>
        futureList.foldLeft(JsArray(List()))((obj, item) => obj ++ Json.arr(item))
      }
    }
  }

  implicit class CursorExtensions[T](val cursor: Cursor[T]) extends AnyVal {

    /**
     * Convert from a List[T] to JsArray[T]
     * @param ec
     * @param writes
     * @return
     */
    def toJsArray(implicit ec: ExecutionContext, writes: Writes[T]): Future[JsArray] = {
      cursor.toList.toJsArray
    }
  }
}
