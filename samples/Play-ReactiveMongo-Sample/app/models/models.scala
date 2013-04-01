package models

case class User(
  age: Int,
  firstName: String,
  lastName: String,
  feeds: List[Feed])

case class Feed(
  name: String,
  url: String)

object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val feedFormat = Json.format[Feed]
  implicit val userFormat = Json.format[User]

  val userForm = Form(
    mapping(
      "age" -> number,
      "firstName" -> text,
      "lastName" -> text,
      "feeds" -> list(feedForm.mapping))(User.apply _)(User.unapply _))
  val feedForm = Form(mapping("name" -> text, "url" -> text)(Feed.apply _)(Feed.unapply _))
}