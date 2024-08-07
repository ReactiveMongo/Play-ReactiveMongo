import play.api.mvc.PathBindable

import reactivemongo.api.bson.{
  BSONBoolean,
  BSONDateTime,
  BSONDouble,
  BSONLong,
  BSONObjectID,
  BSONString,
  BSONSymbol,
  BSONTimestamp
}

final class PathBindableSpec extends org.specs2.mutable.Specification {
  "Play PathBindables".title

  import play.modules.reactivemongo.PathBindables._

  "BSONBoolean" should {
    val bindable = implicitly[PathBindable[BSONBoolean]]

    "be bound" in {
      bindable.bind("foo", "true") must beRight(BSONBoolean(true)) and (
        bindable.bind("bar", "false") must beRight(BSONBoolean(false))
      ) and (
        bindable.bind("lorem", "ipsum") must beLeft
      )
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        "Cannot parse parameter foo as Boolean: should be true, false, 0 or 1"
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BSONBoolean(true)) must ===("true") and (
        bindable.unbind("bar", BSONBoolean(false)) must ===("false")
      )
    }
  }

  "BSONDateTime" should {
    val bindable = implicitly[PathBindable[BSONDateTime]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONDateTime(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BSONDateTime(1234567L)) must ===("1234567")
    }
  }

  "BSONDouble" should {
    val bindable = implicitly[PathBindable[BSONDouble]]

    "be bound" in {
      bindable.bind("foo", "1234.567") must beRight(BSONDouble(1234.567D))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Double: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BSONDouble(1234.567D)) must ===("1234.567")
    }
  }

  "BSONLong" should {
    val bindable = implicitly[PathBindable[BSONLong]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONLong(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BSONLong(1234567L)) must ===("1234567")
    }
  }

  "BSONString" should {
    val bindable = implicitly[PathBindable[BSONString]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONString("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONString("bar")) must ===("bar")
    }
  }

  "BSONSymbol" should {
    val bindable = implicitly[PathBindable[BSONSymbol]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONSymbol("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONSymbol("bar")) must ===("bar")
    }
  }

  "BSONTimestamp" should {
    val bindable = implicitly[PathBindable[BSONTimestamp]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONTimestamp(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BSONTimestamp(1234567L)) must ===("1234567")
    }
  }

  "BSONObjectID" should {
    val bindable = implicitly[PathBindable[BSONObjectID]]

    "be bound" in {
      BSONObjectID
        .parse("55b3eb7e9d13430362a153bc")
        .aka("expected") must beSuccessfulTry[BSONObjectID].like {
        case oid =>
          bindable
            .bind("foo", "55b3eb7e9d13430362a153bc")
            .aka("bound") must beRight(oid)
      }
    }

    "fail to be bound" in {
      bindable.bind("foo", "bar") must beLeft(
        "Wrong ObjectId (length != 24): 'bar'"
      )
    }

    "be unbound" in {
      BSONObjectID
        .parse("55b3eb7e9d13430362a153bc")
        .aka("expected") must beSuccessfulTry[BSONObjectID].like {
        case oid =>
          bindable.unbind("foo", oid).aka("unbound") must ===(
            "55b3eb7e9d13430362a153bc"
          )
      }
    }
  }
}
