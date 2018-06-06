import sbt._
import sbt.Keys._

object BuildSettings {
  val buildSettings = Seq(
    organization := "org.reactivemongo",
    scalaVersion := "2.11.12",
    version ~= { ver =>
      sys.env.get("RELEASE_SUFFIX") match {
        case Some(suffix) => ver.span(_ != '-') match {
          case (a, b) => s"${a}-${suffix}${b}"
        }
        case _ => ver
      }
    },
    crossScalaVersions := Seq(scalaVersion.value, "2.12.6"),
    crossVersion := CrossVersion.binary,
    javacOptions in (Compile, compile) ++= Seq(
      "-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8"),
    scalacOptions in Compile ++= Seq(
      "-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-numeric-widen",
      "-Ywarn-unused-import", "-Ywarn-value-discard", "-Ywarn-dead-code",
      "-Ywarn-unused", "-Xlint:missing-interpolator"),
    Compile / doc / sources := {
      val compiled = (Compile / doc / sources).value

      if (scalaVersion.value startsWith "2.12") {
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
  lazy val playUpper = "2.6.1"
  lazy val playVer = Def.setting[String] {
    sys.env.get("PLAY_VERSION").getOrElse {
      if (scalaVersion.value startsWith "2.11.") playLower
      else playUpper
    }
  }

  private lazy val playDir = Def.setting[String] {
    if (playVer.value startsWith "2.6") "play-2.6"
    else "play-upto2.5"
  }
}

object Format {
  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences)

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, false).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, false).
      setPreference(CompactStringConcatenation, false).
      setPreference(DoubleIndentConstructorArguments, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, false).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, false).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(DanglingCloseParenthesis, Preserve).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesAroundMultiImports, true).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

object Travis {
  val travisEnv = taskKey[Unit]("Print Travis CI env")

  val travisSnapshotBranches =
    SettingKey[Seq[String]]("branches that can be published on sonatype")

  // TODO: Review | remove
  val travisCommand = Command.command("publishSnapshotsFromTravis") { state =>
    val extracted = Project extract state
    import extracted._
    import scala.util.Properties.isJavaAtLeast

    val thisRef = extracted.get(thisProjectRef)

    val isSnapshot = getOpt(version).exists(_.endsWith("SNAPSHOT"))
    val isTravisEnabled = sys.env.get("TRAVIS").exists(_ == "true")
    val isNotPR = sys.env.get("TRAVIS_PULL_REQUEST").exists(_ == "false")
    val isBranchAcceptable = sys.env.get("TRAVIS_BRANCH").exists(branch => getOpt(travisSnapshotBranches).exists(_.contains(branch)))
    val isJavaVersion = !isJavaAtLeast("1.7")

    if (isSnapshot && isTravisEnabled && isNotPR && isBranchAcceptable) {
      println(s"publishing $thisRef from travis...")

      val newState = appendWithoutSession(Seq(
        publishTo := Some("Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"),
        credentials := Seq(Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          sys.env.getOrElse("SONATYPE_USER",
            throw new RuntimeException("no SONATYPE_USER defined")),
          sys.env.getOrElse("SONATYPE_PASSWORD",
            throw new RuntimeException("no SONATYPE_PASSWORD defined"))
        ))),
        state
      )

      runTask(publish in thisRef, newState)

      println(s"published $thisRef from travis")
    } else {
      println(s"not publishing $thisRef to Sonatype: isSnapshot=$isSnapshot, isTravisEnabled=$isTravisEnabled, isNotPR=$isNotPR, isBranchAcceptable=$isBranchAcceptable, javaVersionLessThen_1_7=$isJavaVersion")
    }

    state
  }

  val settings = Seq(
    Travis.travisSnapshotBranches := Seq("master"),
    commands += Travis.travisCommand,
    travisEnv in Test := { // test:travisEnv from SBT CLI
      import BuildSettings.{ playLower, playUpper }

      val specs = List[(String, List[String])](
        "PLAY_VERSION" -> List(playLower, playUpper)
      )

      lazy val integrationEnv = specs.flatMap {
        case (key, values) => values.map(key -> _)
      }.combinations(specs.size).toList

      @inline def integrationVars(flags: List[(String, String)]): String =
        flags.map { case (k, v) => s"$k=$v" }.mkString(" ")

      def integrationMatrix =
        integrationEnv.map(integrationVars).map { c => s"  - $c" }

      def matrix = (("env:" +: integrationMatrix :+
        "matrix: " :+ "  exclude: ") ++ (
        integrationEnv.flatMap { flags =>
          if (/* time-compat exclusions: */
            flags.contains("PLAY_VERSION" -> playUpper)) {
            List(
              "    - scala: 2.11.12",
              s"      env: ${integrationVars(flags)}"
            )
          } else if (/* time-compat exclusions: */
            flags.contains("PLAY_VERSION" -> playLower)) {
            List(
              "    - scala: 2.12.6",
              s"      env: ${integrationVars(flags)}"
            )
          } else List.empty[String]
        })
      ).mkString("\r\n")

      println(s"# Travis CI env\r\n$matrix")
    }
  )
}
