package play.modules.reactivemongo

import org.scalatest.{WordSpec, Matchers}
import play.api.{PlayException, Configuration}
import scala.math._
import reactivemongo.api.FailoverStrategy
import scala.Some

class DelayFactorSpec extends WordSpec with Matchers {

  "Configuration key 'delay'" should {
    "default to FailoverStrategy#delayFactor if not set" in {
      DelayFactor(None).apply(1) shouldBe FailoverStrategy().delayFactor(1)
    }

    "return the 'factor' value for static function" in {
      val conf = Some(Configuration.from(Map("factor" -> 2.0, "function" -> "static")))

      DelayFactor(conf).apply(9999) shouldBe 2.0
    }

    "return the default 'factor' value" in {
      val conf = Some(Configuration.from(Map("function" -> "static")))

      DelayFactor(conf).apply(9999) shouldBe 1.0
    }

    "return linear value" in {
      val conf = Some(Configuration.from(Map("factor" -> 2.0, "function" -> "linear")))

      DelayFactor(conf).apply(1) shouldBe 2.0
    }

    "return exponential value" in {
      val conf = Some(Configuration.from(Map("factor" -> 2.0, "function" -> "exponential")))

      DelayFactor(conf).apply(2) shouldBe 4.0
    }

    "return fibonacci value" in {
      pending
    }

    "fail for unknown function" in {
      val conf = Some(Configuration.from(Map("function" -> "unknown")))

      intercept[PlayException]{
        DelayFactor(conf)
      }
    }
  }
}