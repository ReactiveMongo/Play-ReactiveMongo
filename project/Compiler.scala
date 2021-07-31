import sbt._
import sbt.Keys._

object Compiler {
  val settings = Seq(
    Compile / unmanagedSourceDirectories += {
      val base = (Compile / sourceDirectory).value

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
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13") Nil
      else Seq(
        "-Ywarn-infer-any",
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard")
    },
    scalacOptions ++= {
      // See https://github.com/scala/bug/issues/11880#issuecomment-583682673
      if (scalaBinaryVersion.value == "2.12") {
        Seq("-no-specialization")
      } else Seq.empty
    },
    scalacOptions ++= {
      if (scalaBinaryVersion.value != "2.11") {
        Seq("-Xlint:missing-interpolator")
      } else Seq(
        "-Yconst-opt",
        "-Yclosure-elim",
        "-Ydead-code",
        "-Yopt:_"
      )
    },
    scalacOptions += "-target:jvm-1.8",
    Compile / console / scalacOptions ~= {
      _.filterNot(excludeOpt)
    },
    Test / console / scalacOptions ~= {
      _.filterNot(excludeOpt)
    },
    Test / console / scalacOptions += "-Yrepl-class-based",
  )

  private lazy val excludeOpt: String => Boolean = { opt =>
    opt.startsWith("-X") || opt.startsWith("-Y") ||
    opt.startsWith("-P:silencer")
  }
}
