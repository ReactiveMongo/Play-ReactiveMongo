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

import play.api.libs.json.{ Json, JsObject, OWrites }

import reactivemongo.api.commands.{
  FindAndModifyCommand,
  ResolvedCollectionCommand
}
import play.modules.reactivemongo.json.JSONSerializationPack

object JSONFindAndModifyCommand extends FindAndModifyCommand[JSONSerializationPack.type] {
  val pack: JSONSerializationPack.type = JSONSerializationPack
}

object JSONFindAndModifyImplicits {
  import JSONFindAndModifyCommand._

  implicit object FindAndModifyResultReader extends DealingWithGenericCommandErrorsReader[FindAndModifyResult] {
    def readResult(result: JsObject): FindAndModifyResult =
      FindAndModifyResult(
        (result \ "lastErrorObject").asOpt[JsObject].map { doc =>
          UpdateLastError(
            updatedExisting = (doc \ "updatedExisting").
              asOpt[Boolean].getOrElse(false),
            n = (doc \ "n").asOpt[Int].getOrElse(0),
            err = (doc \ "err").asOpt[String],
            upsertedId = (doc \ "upserted").toOption)
        },
        (result \ "value").asOpt[JsObject])
  }

  implicit object FindAndModifyWriter
      extends OWrites[ResolvedCollectionCommand[FindAndModify]] {

    def writes(command: ResolvedCollectionCommand[FindAndModify]): JsObject =
      Json.obj(
        "findAndModify" -> command.collection,
        "query" -> command.command.query,
        "sort" -> command.command.sort,
        "fields" -> command.command.fields,
        "upsert" -> (if (command.command.upsert) Some(true) else None)) ++ (
          command.command.modify match {
            case Update(document, fetchNewObject) =>
              Json.obj("update" -> document, "new" -> fetchNewObject)

            case Remove => Json.obj("remove" -> true)
          })
  }
}
