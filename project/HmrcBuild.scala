import sbt._
import sbt.Keys._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.{SbtAutoBuildPlugin}

object HmrcBuild extends Build {

  import uk.gov.hmrc.DefaultBuildSettings
  import DefaultBuildSettings._

  lazy val pluginDependencies = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % "2.6.1" % "provided",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "2.6.1" % "test" classifier "tests",

    "com.typesafe.play" %% "play" % "2.3.8" % "provided",
    "com.typesafe.play" %% "play-test" % "2.3.8" % "test",

    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.pegdown" % "pegdown" % "1.5.0" % "test"
  )

  lazy val playReactiveMongo = Project("Play-ReactiveMongo", file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      targetJvm := "jvm-1.7",
      libraryDependencies ++= pluginDependencies,
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      crossScalaVersions := Seq("2.11.6")
    )
}
