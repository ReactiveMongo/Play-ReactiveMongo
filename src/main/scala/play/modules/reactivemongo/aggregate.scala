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
package play.modules.reactivemongo.json.commands

import play.api.libs.json.{ JsValue, Json }

import reactivemongo.api.commands.AggregationFramework
import reactivemongo.play.json.JSONSerializationPack

@deprecated(
  "Use [[reactivemongo.play.json.commands.JSONAggregationFramework]]", "0.12.0")
object JSONAggregationFramework
    extends AggregationFramework[JSONSerializationPack.type] {

  import Json.JsValueWrapper

  val pack: JSONSerializationPack.type = JSONSerializationPack

  protected def makeDocument(elements: Seq[(String, JsValueWrapper)]) =
    Json.obj(elements: _*)

  protected def elementProducer(name: String, value: JsValue) =
    name -> value

  protected def booleanValue(b: Boolean): JsValue = Json.toJson(b)
  protected def intValue(i: Int): JsValue = Json.toJson(i)
  protected def longValue(l: Long): JsValue = Json.toJson(l)
  protected def doubleValue(d: Double): JsValue = Json.toJson(d)
  protected def stringValue(s: String): JsValue = Json.toJson(s)
}

@deprecated(
  "Use [[reactivemongo.play.json.commands.JSONAggregationImplicits]]", "0.12.0")
object JSONAggregationImplicits {
  import play.api.libs.json.{ JsArray, JsObject, JsValue, OWrites }
  import reactivemongo.api.commands.ResolvedCollectionCommand
  import JSONAggregationFramework.{ Aggregate, AggregationResult }
  import play.modules.reactivemongo.json.BSONFormats

  implicit object AggregateWriter
      extends OWrites[ResolvedCollectionCommand[Aggregate]] {
    def writes(agg: ResolvedCollectionCommand[Aggregate]): JsObject = {
      val fields = Map[String, JsValue](
        "aggregate" -> Json.toJson(agg.collection),
        "pipeline" -> JsArray(agg.command.pipeline.map(_.makePipe)),
        "explain" -> Json.toJson(agg.command.explain),
        "allowDiskUse" -> Json.toJson(agg.command.allowDiskUse))

      val optFields: List[(String, JsValue)] = List(
        agg.command.cursor.map(c => "cursor" -> Json.toJson(c.batchSize))).
        flatten

      JsObject(fields ++ optFields)
    }
  }

  implicit object AggregationResultReader
      extends DealingWithGenericCommandErrorsReader[AggregationResult] {
    def readResult(doc: JsObject): AggregationResult =
      AggregationResult((doc \ "result").as[List[JsObject]])

  }
}
