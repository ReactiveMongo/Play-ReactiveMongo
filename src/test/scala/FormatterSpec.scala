import play.api.data.format.Formatter
import play.api.libs.json.Json.stringify
import play.modules.reactivemongo.Formatters._

import reactivemongo.api.bson._

import reactivemongo.play.json.compat.ValueConverters

final class FormatterSpec extends org.specs2.mutable.Specification {
  "Play Formatters".title

  "String formatter" should {
    val formatter = implicitly[Formatter[BSONString]]
    val bson = BSONString("bar")
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", BSONString("bar")) must ===(binding)
    }
  }

  "Date/time formatter" should {
    val formatter = implicitly[Formatter[BSONDateTime]]
    val bson = BSONDateTime(1234L)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Timestamp formatter" should {
    val formatter = implicitly[Formatter[BSONTimestamp]]
    val bson = BSONTimestamp(5678L)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Binary formatter" should {
    val formatter = implicitly[Formatter[BSONBinary]]
    val bson = BSONBinary(Array[Byte](1, 2, 3), Subtype.UserDefinedSubtype)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Regex formatter" should {
    val formatter = implicitly[Formatter[BSONRegex]]
    val bson = BSONRegex("regex", "g")
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Double formatter" should {
    val formatter = implicitly[Formatter[BSONDouble]]
    val bson = BSONDouble(1.23D)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Integer formatter" should {
    val formatter = implicitly[Formatter[BSONInteger]]
    val bson = BSONInteger(123)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Long formatter" should {
    val formatter = implicitly[Formatter[BSONLong]]
    val bson = BSONLong(Long.MaxValue)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Boolean formatter" should {
    val formatter = implicitly[Formatter[BSONBoolean]]
    val bson = BSONBoolean(false)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Null formatter" should {
    val formatter = implicitly[Formatter[BSONNull.type]]
    val bson = BSONNull
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Symbol formatter" should {
    val formatter = implicitly[Formatter[BSONSymbol]]
    val bson = BSONSymbol("sym")
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Array formatter" should {
    val formatter = implicitly[Formatter[BSONArray]]
    val bson = BSONArray(BSONString("lorem"), BSONDouble(1.2D))
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Document formatter" should {
    val formatter = implicitly[Formatter[BSONDocument]]
    val bson = BSONDocument("lorem" -> "ipsum", "bolo" -> 2)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", bson) must ===(binding)
    }
  }

  "Number-like formatter" should {
    val formatter = implicitly[Formatter[BSONNumberLike]]
    val bson = BSONInteger(123)
    val like = implicitly[BSONNumberLike](bson)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", like) must ===(binding)
    }
  }

  "Boolean-like formatter" should {
    val formatter = implicitly[Formatter[BSONBooleanLike]]
    val bson = BSONBoolean(true)
    val like = implicitly[BSONBooleanLike](bson)
    val binding = Map("foo" -> stringify(ValueConverters.fromValue(bson)))

    "bind" in {
      formatter.bind("foo", binding) must beRight(bson)
    }

    "unbind" in {
      formatter.unbind("foo", like) must ===(binding)
    }
  }
}
