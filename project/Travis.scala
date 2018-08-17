import sbt._
import sbt.Keys._

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
      import Common.{ playLower, playUpper }

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
