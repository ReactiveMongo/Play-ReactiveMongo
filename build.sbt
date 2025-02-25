import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters,
  mimaPreviousArtifacts
}

// TODO: Rename play2- suffix to play-

val specs2Dependencies = Def.setting[Seq[ModuleID]] {
  val specsVersion = {
    if (scalaBinaryVersion.value startsWith "3") {
      "5.5.3"
    } else {
      "4.10.6"
    }
  }

  Seq("specs2-core", "specs2-junit").map { n =>
    "org.specs2" %% n % specsVersion % Test
  }
}

val playDependencies = Def.setting[Seq[ModuleID]] {
  val scalaVer = scalaBinaryVersion.value
  val ver = Common.playVer.value
  val base = Seq(
    "play" -> Provided,
    "play-test" -> Test
  )

  val groupId = {
    if (ver.startsWith("2.") && !ver.startsWith("2.10")) {
      "com.typesafe.play"
    } else {
      "org.playframework"
    }
  }

  base.map {
    case (name, scope) =>
      (groupId %% name % ver % scope) cross CrossVersion.binary
  }
}

lazy val reactivemongo = Project("Play2-ReactiveMongo", file(".")).settings(
  Seq(
    resolvers ++= Resolver.sonatypeOssRepos({
      if (version.value endsWith "-SNAPSHOT") "snapshots"
      else "staging"
    }),
    scalacOptions ++= {
      if (scalaBinaryVersion.value != "3") {
        Seq("-P:silencer:globalFilters=.*JSONException.*")
      } else {
        Seq.empty
      }
    },
    libraryDependencies ++= {
      val silencerVer = "1.7.19" // 13
      val v = scalaBinaryVersion.value

      val additionalDeps = {
        if (v != "2.13" && v != "3") {
          Seq("com.typesafe.play" %% "play-iteratees" % "2.6.1" % Provided)
        } else {
          Seq.empty
        }
      }

      val driverDeps = {
        val dv = Common.driverVersion.value

        val dep = ("org.reactivemongo" %% "reactivemongo" % dv)
          .cross(CrossVersion.binary)
          .exclude("com.typesafe.akka", "*")
          .exclude("org.apache.pekko", "*")
          . // provided by Play
          exclude("com.typesafe.play", "*")

        if (Common.useShaded.value) {
          Seq(dep)
        } else {
          Seq(dep, "io.netty" % "netty-handler" % "4.1.43.Final" % Provided)
        }
      }

      def silencer: Seq[ModuleID] = {
        if (v != "3") {
          Seq(
            compilerPlugin(
              ("com.github.ghik" %% "silencer-plugin" % silencerVer)
                .cross(CrossVersion.full)
            ),
            ("com.github.ghik" %% "silencer-lib" % silencerVer % Provided)
              .cross(CrossVersion.full)
          )
        } else {
          Seq.empty
        }
      }

      val buildVer = (ThisBuild / version).value
      val ver = version.value // != buildVer (includes play suffix)

      val jsonCompat =
        ("org.reactivemongo" %% "reactivemongo-play-json-compat" % ver)
          .cross(CrossVersion.binary)
          .exclude("org.reactivemongo", "*") // Avoid mixing shaded w/ nonshaded

      val reactiveMongoStreaming = {
        if (Common.playVer.value startsWith "2.") {
          ("org.reactivemongo" %% "reactivemongo-akkastream" % buildVer)
            .cross(CrossVersion.binary)
            .exclude("com.typesafe.akka", "*") // provided by Play
        } else {
          ("org.reactivemongo" %% "reactivemongo-pekkostream" % buildVer)
            .cross(CrossVersion.binary)
            .exclude("org.apache.pekko", "*") // provided by Play
        }
      }

      driverDeps ++ Seq(
        jsonCompat,
        reactiveMongoStreaming,
        "junit" % "junit" % "4.13.2" % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.13" % Test
      ) ++ additionalDeps ++ playDependencies.value ++ specs2Dependencies.value ++ silencer
    },
    mimaBinaryIssueFilters ++= Seq.empty
  )
)
