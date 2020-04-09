import com.github.ghik.silencer.silent

import reactivemongo.bson._

import play.api.mvc.PathBindable

import reactivemongo.api.bson.{
  BSONDateTime => BisonDateTime,
  BSONDouble => BisonDouble,
  BSONBoolean => BisonBoolean,
  BSONLong => BisonLong,
  BSONString => BisonString,
  BSONSymbol => BisonSymbol,
  BSONTimestamp => BisonTimestamp,
  BSONObjectID => BisonObjectID
}

final class PathBindableSpec extends org.specs2.mutable.Specification {
  "Play PathBindables" title

  import play.modules.reactivemongo.PathBindables._

  "BSONBoolean" should {
    @silent(".*deprecated.*")
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
      bindable.unbind("foo", BSONBoolean(true)) must_=== "true" and (
        bindable.unbind("bar", BSONBoolean(false)) must_=== "false"
      )
    }
  }

  "BSONDateTime" should {
    @silent(".*deprecated.*")
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
      bindable.unbind("foo", BSONDateTime(1234567L)) must_=== "1234567"
    }
  }

  "BSONDouble" should {
    @silent(".*deprecated.*")
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
      bindable.unbind("foo", BSONDouble(1234.567D)) must_=== "1234.567"
    }
  }

  "BSONLong" should {
    @silent(".*deprecated.*")
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
      bindable.unbind("foo", BSONLong(1234567L)) must_=== "1234567"
    }
  }

  "BSONString" should {
    @silent(".*deprecated.*")
    val bindable = implicitly[PathBindable[BSONString]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONString("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONString("bar")) must_=== "bar"
    }
  }

  "BSONSymbol" should {
    @silent(".*deprecated.*")
    val bindable = implicitly[PathBindable[BSONSymbol]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONSymbol("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONSymbol("bar")) must_=== "bar"
    }
  }

  "BSONTimestamp" should {
    @silent(".*deprecated.*")
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
      bindable.unbind("foo", BSONTimestamp(1234567L)) must_=== "1234567"
    }
  }

  "BSONObjectID" should {
    @silent(".*deprecated.*")
    val bindable = implicitly[PathBindable[BSONObjectID]]

    "be bound" in {
      BSONObjectID.parse("55b3eb7e9d13430362a153bc").
        aka("expected") must beSuccessfulTry[BSONObjectID].like {
          case oid => bindable.bind("foo", "55b3eb7e9d13430362a153bc").
            aka("bound") must beRight(oid)
        }
    }

    "fail to be bound" in {
      bindable.bind("foo", "bar") must beLeft(
        "Wrong ObjectId (length != 24): 'bar'"
      )
    }

    "be unbound" in {
      BSONObjectID.parse("55b3eb7e9d13430362a153bc").
        aka("expected") must beSuccessfulTry[BSONObjectID].like {
          case oid => bindable.unbind("foo", oid).
            aka("unbound") must_=== "55b3eb7e9d13430362a153bc"
        }
    }
  }

  // ---

  "BisonBoolean" should {
    val bindable = implicitly[PathBindable[BisonBoolean]]

    "be bound" in {
      bindable.bind("foo", "true") must beRight(BisonBoolean(true)) and (
        bindable.bind("bar", "false") must beRight(BisonBoolean(false))
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
      bindable.unbind("foo", BisonBoolean(true)) must_=== "true" and (
        bindable.unbind("bar", BisonBoolean(false)) must_=== "false"
      )
    }
  }

  "BisonDateTime" should {
    val bindable = implicitly[PathBindable[BisonDateTime]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BisonDateTime(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BisonDateTime(1234567L)) must_=== "1234567"
    }
  }

  "BisonDouble" should {
    val bindable = implicitly[PathBindable[BisonDouble]]

    "be bound" in {
      bindable.bind("foo", "1234.567") must beRight(BisonDouble(1234.567D))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Double: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BisonDouble(1234.567D)) must_=== "1234.567"
    }
  }

  "BisonLong" should {
    val bindable = implicitly[PathBindable[BisonLong]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BisonLong(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BisonLong(1234567L)) must_=== "1234567"
    }
  }

  "BisonString" should {
    val bindable = implicitly[PathBindable[BisonString]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BisonString("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BisonString("bar")) must_=== "bar"
    }
  }

  "BisonSymbol" should {
    val bindable = implicitly[PathBindable[BisonSymbol]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BisonSymbol("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BisonSymbol("bar")) must_=== "bar"
    }
  }

  "BisonTimestamp" should {
    val bindable = implicitly[PathBindable[BisonTimestamp]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BisonTimestamp(1234567L))
    }

    "fail to be bound" in {
      bindable.bind("foo", "abc") must beLeft(
        """Cannot parse parameter foo as Long: For input string: "abc""""
      )
    }

    "be unbound" in {
      bindable.unbind("foo", BisonTimestamp(1234567L)) must_=== "1234567"
    }
  }

  "BisonObjectID" should {
    val bindable = implicitly[PathBindable[BisonObjectID]]

    "be bound" in {
      BisonObjectID.parse("55b3eb7e9d13430362a153bc").
        aka("expected") must beSuccessfulTry[BisonObjectID].like {
          case oid => bindable.bind("foo", "55b3eb7e9d13430362a153bc").
            aka("bound") must beRight(oid)
        }
    }

    "fail to be bound" in {
      bindable.bind("foo", "bar") must beLeft(
        "Wrong ObjectId (length != 24): 'bar'"
      )
    }

    "be unbound" in {
      BisonObjectID.parse("55b3eb7e9d13430362a153bc").
        aka("expected") must beSuccessfulTry[BisonObjectID].like {
          case oid => bindable.unbind("foo", oid).
            aka("unbound") must_=== "55b3eb7e9d13430362a153bc"
        }
    }
  }
}
