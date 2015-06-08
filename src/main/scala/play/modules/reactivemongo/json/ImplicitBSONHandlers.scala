package play.modules.reactivemongo.json

import play.api.libs.json.{ JsObject, JsValue }
import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONDocumentWriter }

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
object ImplicitBSONHandlers extends ImplicitBSONHandlers

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

trait LowerImplicitBSONHandlers {
  implicit object JsValueWriter extends BSONDocumentWriter[JsValue] {
    def write(jsValue: JsValue) = BSONFormats.BSONDocumentFormat.reads(jsValue).get
  }
  implicit object JsValueReader extends BSONDocumentReader[JsValue] {
    def read(document: BSONDocument) = BSONFormats.BSONDocumentFormat.writes(document)
  }
}