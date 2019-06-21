import sbt._
import sbt.Keys._

object Compiler {
  val settings = Seq(
    unmanagedSourceDirectories in Compile += {
      val base = (sourceDirectory in Compile).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    },
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Xlint",
      "-g:vars"
    ),
    scalacOptions in Compile ++= {
      if (scalaVersion.value startsWith "2.13.") Nil
      else Seq(
        "-Ywarn-infer-any",
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard")
    },
    scalacOptions in Compile ++= {
      if (!scalaVersion.value.startsWith("2.11.")) {
        Seq("-Xlint:missing-interpolator")
      } else Seq(
        "-Yconst-opt",
        "-Yclosure-elim",
        "-Ydead-code",
        "-Yopt:_"
      )
    },
    scalacOptions in Compile += "-target:jvm-1.8",
    scalacOptions in (Compile, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    scalacOptions in (Test, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    scalacOptions in (Test, console) += "-Yrepl-class-based"
  )
}
