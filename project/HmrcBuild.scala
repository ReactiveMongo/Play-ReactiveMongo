import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

  lazy val pluginDependencies = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % "5.2.0",

    "com.typesafe.play" %% "play" % "2.5.12" % "provided",
    "com.typesafe.play" %% "play-test" % "2.5.12" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.5.12" % "test",

    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.pegdown" % "pegdown" % "1.5.0" % "test"
  )

  lazy val playReactiveMongo = Project("Play-ReactiveMongo", file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.7",
      libraryDependencies ++= pluginDependencies,
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases")
      ),
      crossScalaVersions := Seq("2.11.7")
    )
}
