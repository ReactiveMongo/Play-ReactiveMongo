package play.modules.reactivemongo

import play.api.mvc.{ AbstractController => Ctrl }

object PlaySupport {
  type Controller = Ctrl

  object FileInfo {
    import play.core.parsers.Multipart

    def unapply(that: Any): Option[(String, String, Option[String])] =
      that match {
        case Multipart.FileInfo(partName, fileName, contentType, _) =>
          Some(Tuple3(partName, fileName, contentType))

        case _ => Option.empty
      }
  }
}
