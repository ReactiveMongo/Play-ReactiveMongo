/*
 * Copyright 2012 Pascal Voitot
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

import play.api.data.resource._
import play.api.libs.json._
import org.asyncmongo.bson._
import org.asyncmongo.handlers._
import org.asyncmongo.handlers.DefaultBSONHandlers._
import play.api.libs.concurrent._
import org.asyncmongo.protocol.commands._
import org.asyncmongo.api.Cursor
import play.api.libs.iteratee.Enumerator

import PlayBsonImplicits._

case class MongoTemplate[T](collection: String)(implicit bw: BSONWriter[T], br: BSONReader[T]) extends ResourceTemplate[T] {
  import play.api.Play.current

  val coll = MongoAsyncPlugin.collection(collection)

  def insert(t: T): Promise[ResourceResult[T]] = {
    coll.insert(t, GetLastError()).asPromise.map { lastError =>
      if(lastError.ok) ResourceSuccess(t)
      else ResourceOpError(Seq(ResourceErrorMsg("DB_ERROR", "resource.error.insert", lastError.stringify)))
    }
  }

  def findOne(json: JsValue): Promise[ResourceResult[T]] = {
    val future = coll.find[JsValue, JsValue, T](json, None, 0, 0)

    future.asPromise.map( cur => 
      if(cur.iterator.hasNext) ResourceSuccess(cur.iterator.next)
      else ResourceOpError(Seq(ResourceErrorMsg("DB_ERROR", "resource.error.notfound")))
    )
  }

  def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]] = {
    val future = coll.find[JsValue, JsValue, T](json, None, 0, 0)

    Promise.pure(ResourceSuccess(Cursor.enumerate(Some(future))))
  }
} 