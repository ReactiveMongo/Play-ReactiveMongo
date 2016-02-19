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

import play.api.libs.json.{
  JsError,
  JsNumber,
  JsObject,
  JsResult,
  JsSuccess,
  JsValue,
  Reads
}

import reactivemongo.api.commands.{ CommandError, UnitBox }

import play.modules.reactivemongo.json.JSONSerializationPack

@deprecated(
  "Use [[reactivemongo.play.json.commands.CommonImplicits]]", "0.12.0")
object CommonImplicits {
  implicit object UnitBoxReader
      extends DealingWithGenericCommandErrorsReader[UnitBox.type] {
    def readResult(doc: JsObject): UnitBox.type = UnitBox
  }
}

@deprecated(
  "Use [[reactivemongo.play.json.commands.JSONCommandError]]", "0.12.0")
trait JSONCommandError extends CommandError {
  def originalDocument: JsObject
}

@deprecated(
  "Use [[reactivemongo.play.json.commands.DefaultJSONCommandError]]", "0.12.0")
case class DefaultJSONCommandError(
    code: Option[Int],
    errmsg: Option[String],
    originalDocument: JsObject) extends JSONCommandError {
  override def getMessage = s"CommandError[code=${code.getOrElse("<unknown>")}, errmsg=${errmsg.getOrElse("<unknown>")}, doc: ${originalDocument}]"
}

private[commands] trait DealingWithGenericCommandErrorsReader[A]
    extends Reads[A] {

  def readResult(doc: JsObject): A

  final def reads(json: JsValue): JsResult[A] = json match {
    case doc: JsObject => {
      if (!(doc \ "ok").asOpt[JsNumber].exists(_.value.toInt == 1)) {
        JsError(new DefaultJSONCommandError(
          code = (doc \ "code").asOpt[Int],
          errmsg = (doc \ "errmsg").asOpt[String],
          originalDocument = doc).getMessage())
      } else JsSuccess(readResult(doc))
    }

    case v => JsError(s"Expecting a ReactiveMongo document, found: $v")
  }
}
