import sbt.Keys._
import sbt._

object Compiler {

  private val silencerVer = Def.setting[String] {
    "1.7.8"
  }

  lazy val settings = Seq(
    scalaVersion := "2.12.16",
    crossScalaVersions := Seq(
      "2.11.12",
      scalaVersion.value,
      "2.13.7",
      "3.1.3-RC2"
    ),
    ThisBuild / crossVersion := CrossVersion.binary,
    Compile / unmanagedSourceDirectories += {
      val base = (Compile / sourceDirectory).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n < 13 => base / "scala-2.13-"
        case _                      => base / "scala-2.13+"
      }
    },
    Test / unmanagedSourceDirectories += {
      val base = (Test / sourceDirectory).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n < 13 => base / "scala-2.13-"
        case _                      => base / "scala-2.13+"
      }
    },
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-language:higherKinds"
    ),
    scalacOptions ++= {
      if (scalaBinaryVersion.value startsWith "2.") {
        Seq(
          "-target:jvm-1.8",
          "-Xlint",
          "-g:vars"
        )
      } else Seq()
    },
    scalacOptions ++= {
      val sv = scalaBinaryVersion.value

      if (sv == "2.12") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-Ywarn-value-discard",
          "-Ywarn-infer-any",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Xlint:missing-interpolator",
          "-Ywarn-macros:after"
        )
      } else if (sv == "2.11") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Yopt:_",
          "-Ydead-code",
          "-Yclosure-elim",
          "-Yconst-opt"
        )
      } else if (sv == "2.13") {
        Seq(
          "-explaintypes",
          "-Werror",
          "-Wnumeric-widen",
          "-Wdead-code",
          "-Wvalue-discard",
          "-Wextra-implicit",
          "-Wmacros:after",
          "-Wunused"
        )
      } else {
        Seq("-Wunused:all", "-language:implicitConversions")
      }
    },
    Compile / doc / scalacOptions := (Test / scalacOptions).value,
    Compile / console / scalacOptions ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    Test / console / scalacOptions ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    libraryDependencies ++= {
      if (scalaBinaryVersion.value != "3") {
        Seq(
          compilerPlugin(
            ("com.github.ghik" %% "silencer-plugin" % silencerVer.value)
              .cross(CrossVersion.full)
          ),
          ("com.github.ghik" %% "silencer-lib" % silencerVer.value % Provided)
            .cross(CrossVersion.full)
        )
      } else {
        Seq.empty
      }
    }
  )
}
