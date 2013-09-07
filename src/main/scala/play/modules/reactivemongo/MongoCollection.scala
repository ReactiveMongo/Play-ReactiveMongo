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
import reactivemongo.core.commands.{ GetLastError, LastError }
import play.modules.reactivemongo.json.collection.{ JSONQueryBuilder, JSONGenericHandlers }
import reactivemongo.api.collections.{ GenericQueryBuilder, GenericCollection }
import play.api.libs.json.Json.JsValueWrapper
import scala.reflect.ClassTag

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

// Play Json imports
import play.api.libs.json._

import play.api.Play.current

class MongoCollection[T: ClassTag] extends GenericCollection[JsObject, Reads, Writes] with JSONGenericHandlers with CollectionMetaCommands {

  private def getNameFromClass(implicit ct: ClassTag[T]) = ct.runtimeClass.getSimpleName.toLowerCase + "s"
  def name: String = getNameFromClass
  def db: DB = ReactiveMongoPlugin.db
  def failoverStrategy: FailoverStrategy = FailoverStrategy()

  def genericQueryBuilder: GenericQueryBuilder[JsObject, Reads, Writes] = JSONQueryBuilder(this, failoverStrategy)

  private val ID = "_id"

  implicit private def ec: ExecutionContext = ExecutionContext.Implicits.global

  // Not shure this is a good idea, because it doesn't work when named to find, clashes with the existing find method
  def query(query: (String, JsValueWrapper)*)(implicit ec: ExecutionContext, format: Format[T]): Future[List[T]] = find(Json.obj(query: _*)).cursor[T].toList
  def findOne(query: (String, JsValueWrapper)*)(implicit ec: ExecutionContext, format: Format[T]): Future[Option[T]] = find(Json.obj(query: _*)).one[T]

  def findById(id: BSONObjectID)(implicit ec: ExecutionContext, format: Format[T]): Future[Option[T]] = findOne(ID -> id)
  def findById(id: String)(implicit ec: ExecutionContext, format: Format[T]): Future[Option[T]] = findById(BSONObjectID(id))

  def update(id: BSONObjectID, model: T)(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = update(id, model, GetLastError())(ec, writer)
  def update(id: BSONObjectID, model: T, writeConcern: GetLastError)(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = {
    update(Json.obj(ID -> id), writer.writes(model).as[JsObject], writeConcern, upsert = true)
  }
  def update(id: String, model: T)(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = update(id, model, GetLastError())(ec, writer)
  def update(id: String, model: T, writeConcern: GetLastError)(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = update(BSONObjectID(id), model, writeConcern)

  def remove(query: (String, JsValueWrapper)*)(implicit ec: ExecutionContext): Future[LastError] = remove(query: _*)
  def remove(id: BSONObjectID)(implicit ec: ExecutionContext): Future[LastError] = remove(Json.obj(ID -> id))
  def remove(id: String)(implicit ec: ExecutionContext): Future[LastError] = remove(BSONObjectID(id))

  /*
    Methods from JSONCollection
   */
  def save(doc: JsObject)(implicit ec: ExecutionContext): Future[LastError] = save(doc, GetLastError())
  def save(doc: JsObject, writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    import reactivemongo.bson._
    import play.modules.reactivemongo.json.BSONFormats
    (doc \ "_id" match {
      case JsUndefined(_) => insert(doc + ("_id" -> BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)), writeConcern)
      case id             => update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    })
  }
  def save(model: T, writeConcern: GetLastError = GetLastError())(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = {
    save(writer.writes(model).as[JsObject], writeConcern)
  }
}
