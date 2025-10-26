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
    useShaded := sys.env.get("REACTIVEMONGO_SHADED").fold(true)(_.toBoolean),
    driverVersion := {
      val ver = (ThisBuild / version).value
      val suffix = {
        (useShaded.value, playVer.value startsWith "2.") match {
          case (false, false) => "noshaded.pekko"
          case (true, false)  => "pekko"
          case (false, true)  => "noshaded"
          case (true, true)   => ""
        }
      }

      if (suffix.isEmpty) {
        ver
      } else {
        ver.span(_ != '-') match {
          case (_, "") => s"${ver}-${suffix}"

          case (a, b) => s"${a}-${suffix}.${b stripPrefix "-"}"
        }
      }
    },
    version ~= { ver =>
      val suffix = sys.env.getOrElse("RELEASE_SUFFIX", "")

      if (suffix.isEmpty) {
        ver
      } else {
        ver.span(_ != '-') match {
          case (_, "") => s"${ver}-${suffix}"

          case (a, b) => s"${a}-${suffix}.${b stripPrefix "-"}"
        }
      }
    },
    scalaVersion := "2.12.20",
    crossScalaVersions := Seq(
      "2.11.12",
      scalaVersion.value,
      "2.13.17",
      "3.3.6"
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
    Compile / unmanagedSourceDirectories ++= playDirs.value.map { dir =>
      baseDirectory.value / "src" / "main" / dir
    },
    Test / unmanagedSourceDirectories ++= playDirs.value.map { dir =>
      baseDirectory.value / "src" / "test" / dir
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
  lazy val playUpper = "2.8.22"

  lazy val playVer = Def.setting[String] {
    sys.env.get("PLAY_VERSION").getOrElse {
      if (scalaBinaryVersion.value == "2.11") playLower
      else playUpper
    }
  }

  private lazy val playDirs = Def.setting[Seq[String]] {
    val v = playVer.value
    val dirs = Seq.newBuilder[String]

    dirs += {
      if (v startsWith "2.5") "play-2.6-"
      else s"play-${playVer.value take 3}"
    }

    if (v startsWith "2") {
      dirs += "play-2"
    }

    dirs.result()
  }
}
