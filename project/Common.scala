import sbt._
import sbt.Keys._

object Common {
  val settings = Compiler.settings ++ Seq(
    organization := "org.reactivemongo",
    scalaVersion := "2.12.10",
    version ~= { ver =>
      sys.env.get("RELEASE_SUFFIX") match {
        case Some(suffix) => ver.span(_ != '-') match {
          case (a, b) => s"${a}-${suffix}${b}"
        }
        case _ => ver
      }
    },
    crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.1"),
    crossVersion := CrossVersion.binary,
    javacOptions in (Compile, compile) ++= Seq(
      "-source", "1.8", "-target", "1.8"),
    Compile / doc / sources := {
      val compiled = (Compile / doc / sources).value

      if (scalaBinaryVersion.value == "2.12") {
        compiled.filter { _.getName endsWith "NamedDatabase.java" }
      } else compiled
    },
    Compile / doc / scalacOptions ++= Seq("-unchecked", "-deprecation",
      /*"-diagrams", */"-implicits", "-skip-packages", "samples") ++
      Opts.doc.title("ReactiveMongo Play plugin") ++
      Opts.doc.version(Release.major.value),
    unmanagedSourceDirectories in Compile += {
      baseDirectory.value / "src" / "main" / playDir.value
    },
    unmanagedSourceDirectories in Test += {
      baseDirectory.value / "src" / "test" / playDir.value
    },
    fork in Test := false,
    testOptions in Test += Tests.Cleanup(cl => {
      import scala.language.reflectiveCalls
      val c = cl.loadClass("Common$")
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]
      m.close()
    })
  ) ++ Publish.settings ++ Format.settings ++ Travis.settings ++ (
    Publish.mimaSettings ++ Release.settings)

  lazy val playLower = "2.5.0"
  lazy val playUpper = "2.7.4"
  lazy val playVer = Def.setting[String] {
    sys.env.get("PLAY_VERSION").getOrElse {
      if (scalaVersion.value startsWith "2.11.") playLower
      else playUpper
    }
  }

  private lazy val playDir = Def.setting[String] {
    if (playVer.value startsWith "2.5") "play-2.6-"
    else s"play-${playVer.value take 3}"
  }
}
