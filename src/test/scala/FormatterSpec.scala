import reactivemongo.bson._

import play.api.data.format.Formatter
import play.api.libs.json.Json.{ stringify, toJson }

import play.modules.reactivemongo.json._
import play.modules.reactivemongo.Formatters._

object FormatterSpec extends org.specs2.mutable.Specification {
  "Play Formatters" title

  "String formatter" should {
    val formatter = implicitly[Formatter[BSONString]]
    val bson = BSONString("bar")
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", BSONString("bar")) must_== binding
    }
  }

  "Date/time formatter" should {
    val formatter = implicitly[Formatter[BSONDateTime]]
    val bson = BSONDateTime(1234L)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Timestamp formatter" should {
    val formatter = implicitly[Formatter[BSONTimestamp]]
    val bson = BSONTimestamp(5678L)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Binary formatter" should {
    val formatter = implicitly[Formatter[BSONBinary]]
    val bson = BSONBinary(Array[Byte](1, 2, 3), Subtype.UserDefinedSubtype)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Regex formatter" should {
    val formatter = implicitly[Formatter[BSONRegex]]
    val bson = BSONRegex("regex", "g")
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Double formatter" should {
    val formatter = implicitly[Formatter[BSONDouble]]
    val bson = BSONDouble(1.23D)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Integer formatter" should {
    val formatter = implicitly[Formatter[BSONInteger]]
    val bson = BSONInteger(123)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Long formatter" should {
    val formatter = implicitly[Formatter[BSONLong]]
    val bson = BSONLong(123L)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Boolean formatter" should {
    val formatter = implicitly[Formatter[BSONBoolean]]
    val bson = BSONBoolean(false)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Null formatter" should {
    val formatter = implicitly[Formatter[BSONNull.type]]
    val bson = BSONNull
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Symbol formatter" should {
    val formatter = implicitly[Formatter[BSONSymbol]]
    val bson = BSONSymbol("sym")
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Array formatter" should {
    val formatter = implicitly[Formatter[BSONArray]]
    val bson = BSONArray(BSONString("lorem"), BSONDouble(1.2D))
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Document formatter" should {
    val formatter = implicitly[Formatter[BSONDocument]]
    val bson = BSONDocument("lorem" -> "ipsum", "bolo" -> 2)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must_== binding
    }
  }

  "Number-like formatter" should {
    val formatter = implicitly[Formatter[BSONNumberLike]]
    val bson = BSONInteger(123)
    val like = implicitly[BSONNumberLike](bson)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding).right.map(_.toInt) must beRight(123)
    }

    "unbind" in {
      formatter.unbind("foo", like) must_== binding
    }
  }

  "Boolean-like formatter" should {
    val formatter = implicitly[Formatter[BSONBooleanLike]]
    val bson = BSONBoolean(true)
    val like = implicitly[BSONBooleanLike](bson)
    val binding = Map("foo" -> stringify(toJson(bson)))

    "bind" in {
      formatter.bind("foo", binding).right.map(_.toBoolean) must beRight(true)
    }

    "unbind" in {
      formatter.unbind("foo", like) must_== binding
    }
  }
}
