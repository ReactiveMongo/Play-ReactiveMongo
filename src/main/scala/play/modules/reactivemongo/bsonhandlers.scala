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
package play.modules.reactivemongo.json

import org.jboss.netty.buffer._
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters
import scala.util.{ Failure, Success, Try }

trait LowerImplicitBSONHandlers {
  implicit object JsValueWriter extends BSONDocumentWriter[JsValue] {
    def write(jsValue: JsValue) = BSONFormats.BSONDocumentFormat.reads(jsValue).get
  }
  implicit object JsValueReader extends BSONDocumentReader[JsValue] {
    def read(document: BSONDocument) = BSONFormats.BSONDocumentFormat.writes(document)
  }
}

trait ImplicitBSONHandlers extends LowerImplicitBSONHandlers {
  implicit object JsObjectWriter extends BSONDocumentWriter[JsObject] {
    def write(obj: JsObject): BSONDocument =
      BSONFormats.BSONDocumentFormat.reads(obj).get
  }

  implicit object JsObjectReader extends BSONDocumentReader[JsObject] {
    def read(document: BSONDocument) =
      BSONFormats.BSONDocumentFormat.writes(document).as[JsObject]
  }
}

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
object ImplicitBSONHandlers extends ImplicitBSONHandlers
