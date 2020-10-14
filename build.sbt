import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters, mimaPreviousArtifacts
}

val specsVersion = "4.10.5"
val specs2Dependencies = Seq("specs2-core", "specs2-junit").
  map("org.specs2" %% _ % specsVersion % Test)

val playDependencies = Def.setting[Seq[ModuleID]] {
  val ver = Common.playVer.value
  val base = Seq(
    "play" -> Provided,
    "play-test" -> Test
  )

  val baseDeps = base.map {
    case (name, scope) =>
      ("com.typesafe.play" %% name % ver % scope) cross CrossVersion.binary
  }

  baseDeps
}

lazy val reactivemongo = Project("Play2-ReactiveMongo", file(".")).
  settings(Seq(
    resolvers += Resolver.sonatypeRepo({
      if (version.value endsWith "-SNAPSHOT") "snapshots"
      else "staging"
    }),
    scalacOptions += "-P:silencer:globalFilters=.*JSONException.*",
    libraryDependencies ++= {
      val silencerVer = "1.7.1"

      val additionalDeps = {
        if (scalaBinaryVersion.value != "2.13") {
          Seq(
            "com.typesafe.play" %% "play-iteratees" % "2.6.1" % Provided)
        } else {
          Seq.empty
        }
      }

      val driverDeps = {
        val dep = ("org.reactivemongo" %% "reactivemongo" % (
          Common.driverVersion).value cross CrossVersion.binary).
          exclude("com.typesafe.akka", "*"). // provided by Play
          exclude("com.typesafe.play", "*")

        if (Common.useShaded.value) {
          Seq(dep)
        } else {
          Seq(dep, "io.netty" % "netty-handler" % "4.1.43.Final" % Provided)
        }
      }

      def silencer = Seq(
        compilerPlugin(("com.github.ghik" %% "silencer-plugin" % silencerVer).
          cross(CrossVersion.full)),
        ("com.github.ghik" %% "silencer-lib" % silencerVer % Provided).cross(
          CrossVersion.full))

      driverDeps ++ Seq(
        "org.reactivemongo" %% "reactivemongo-play-json-compat" % (
          version.value) cross CrossVersion.binary,
        "org.reactivemongo" %% "reactivemongo" % (
          Common.driverVersion).value cross CrossVersion.binary,
        "org.reactivemongo" %% "reactivemongo-akkastream" % (
          (version in ThisBuild).value) cross CrossVersion.binary,
        "junit" % "junit" % "4.13" % Test,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.13.3" % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
      ) ++ additionalDeps ++ playDependencies.
        value ++ specs2Dependencies ++ silencer
    },
    mimaBinaryIssueFilters ++= {
      /*
      import ProblemFilters.{ exclude => x }
      @inline def mmp(s: String) = x[MissingMethodProblem](s)
      @inline def imt(s: String) = x[IncompatibleMethTypeProblem](s)
      @inline def irt(s: String) = x[IncompatibleResultTypeProblem](s)
      @inline def mtp(s: String) = x[MissingTypesProblem](s)
      @inline def mcp(s: String) = x[MissingClassProblem](s)
      @inline def rmm(s: String) = x[ReversedMissingMethodProblem](s)
      */

      Seq.empty
    }
  ))
