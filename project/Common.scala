import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object Common extends AutoPlugin {
  // import com.typesafe.tools.mima.core._

  override def trigger = allRequirements
  override def requires = JvmPlugin

  val useShaded = settingKey[Boolean](
    "Use ReactiveMongo-Shaded (see system property 'reactivemongo.shaded')"
  )

  val driverVersion = settingKey[String]("Version of the driver dependency")

  override def projectSettings = Compiler.settings ++ Seq(
    organization := "org.reactivemongo",
    scalaVersion := "2.12.15",
    useShaded := sys.env.get("REACTIVEMONGO_SHADED").fold(true)(_.toBoolean),
    driverVersion := {
      val v = (ThisBuild / version).value
      val suffix = {
        if (useShaded.value) "" // default ~> no suffix
        else "-noshaded"
      }

      v.span(_ != '-') match {
        case (a, b) => s"${a}${suffix}${b}"
      }
    },
    version ~= { ver =>
      sys.env.get("RELEASE_SUFFIX") match {
        case Some(suffix) =>
          ver.split("-").toList match {
            case major :: Nil =>
              s"${major}-${suffix}"

            case vs @ _ =>
              ((vs.init :+ suffix) ++ vs.lastOption.toList).mkString("-")
          }

        case _ => ver
      }
    },
    crossScalaVersions := Seq(
      "2.11.12",
      scalaVersion.value,
      "2.13.7",
      "3.1.2-RC1-bin-20211222-c94b333-NIGHTLY"
    ),
    crossVersion := CrossVersion.binary,
    Compile / compile / javacOptions ++= Seq(
      "-source",
      "1.8",
      "-target",
      "1.8"
    ),
    Compile / doc / sources := {
      val compiled = (Compile / doc / sources).value

      if (scalaBinaryVersion.value == "2.12") {
        compiled.filter { _.getName endsWith "NamedDatabase.java" }
      } else compiled
    },
    Compile / unmanagedSourceDirectories += {
      baseDirectory.value / "src" / "main" / playDir.value
    },
    Test / unmanagedSourceDirectories += {
      baseDirectory.value / "src" / "test" / playDir.value
    },
    Test / fork := false,
    Test / testOptions += Tests.Cleanup(cl => {
      import scala.language.reflectiveCalls
      val c = cl.loadClass("Common$")
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]
      m.close()
    })
  ) ++ Publish.settings ++ Publish.mimaSettings ++ Release.settings

  lazy val playLower = "2.5.0"
  lazy val playUpper = "2.8.15"

  lazy val playVer = Def.setting[String] {
    sys.env.get("PLAY_VERSION").getOrElse {
      if (scalaBinaryVersion.value == "2.11") playLower
      else playUpper
    }
  }

  private lazy val playDir = Def.setting[String] {
    if (playVer.value startsWith "2.5") "play-2.6-"
    else s"play-${playVer.value take 3}"
  }
}
