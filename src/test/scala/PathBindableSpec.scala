import reactivemongo.bson._

import play.api.mvc.PathBindable

object PathBindableSpec extends org.specs2.mutable.Specification {
  "Play PathBindables" title

  import play.modules.reactivemongo.PathBindables._

  "BSONBoolean" should {
    val bindable = implicitly[PathBindable[BSONBoolean]]

    "be bound" in {
      bindable.bind("foo", "true") must beRight(BSONBoolean(true)) and (
        bindable.bind("bar", "false") must beRight(BSONBoolean(false))) and (
          bindable.bind("lorem", "ipsum") must beLeft)
    }

    "be unbound" in {
      bindable.unbind("foo", BSONBoolean(true)) must_== "true" and (
        bindable.unbind("bar", BSONBoolean(false)) must_== "false")
    }
  }

  "BSONDateTime" should {
    val bindable = implicitly[PathBindable[BSONDateTime]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONDateTime(1234567L))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONDateTime(1234567L)) must_== "1234567"
    }
  }

  "BSONDouble" should {
    val bindable = implicitly[PathBindable[BSONDouble]]

    "be bound" in {
      bindable.bind("foo", "1234.567") must beRight(BSONDouble(1234.567D))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONDouble(1234.567D)) must_== "1234.567"
    }
  }

  "BSONLong" should {
    val bindable = implicitly[PathBindable[BSONLong]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONLong(1234567L))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONLong(1234567L)) must_== "1234567"
    }
  }

  "BSONString" should {
    val bindable = implicitly[PathBindable[BSONString]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONString("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONString("bar")) must_== "bar"
    }
  }

  "BSONSymbol" should {
    val bindable = implicitly[PathBindable[BSONSymbol]]

    "be bound" in {
      bindable.bind("foo", "bar") must beRight(BSONSymbol("bar"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONSymbol("bar")) must_== "bar"
    }
  }

  "BSONTimestamp" should {
    val bindable = implicitly[PathBindable[BSONTimestamp]]

    "be bound" in {
      bindable.bind("foo", "1234567") must beRight(BSONTimestamp(1234567L))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONTimestamp(1234567L)) must_== "1234567"
    }
  }

  "BSONObjectID" should {
    val bindable = implicitly[PathBindable[BSONObjectID]]

    "be bound" in {
      bindable.bind("foo", "55b3eb7e9d13430362a153bc").
        aka("bound") must beRight(BSONObjectID("55b3eb7e9d13430362a153bc"))
    }

    "be unbound" in {
      bindable.unbind("foo", BSONObjectID("55b3eb7e9d13430362a153bc")).
        aka("unbound") must_== "55b3eb7e9d13430362a153bc"
    }
  }
}
