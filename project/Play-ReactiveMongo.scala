import sbt._
import sbt.Keys._

object PlayReactiveMongoBuild extends Build {

  import uk.gov.hmrc.DefaultBuildSettings
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}

  val appVersion = "3.4.0-SNAPSHOT"

  val simpleReactiveMongoVersion = "2.1.2"

  lazy val pluginDependencies = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "provided",
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "test" classifier "tests",

    "com.typesafe.play" %% "play" % "[2.2.1,2.3.7]" % "provided",
    "com.typesafe.play" %% "play-test" % "[2.2.1,2.3.7]" % "test",

    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test"
  )

  lazy val playReactiveMongo = Project("Play-ReactiveMongo", file("."))
    .settings(version := appVersion)
    .settings(scalaSettings : _*)
    .settings(defaultSettings() : _*)
    .settings(
      targetJvm := "jvm-1.7",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= pluginDependencies,
      resolvers := Seq(
        Opts.resolver.sonatypeReleases,
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      crossScalaVersions := Seq("2.11.5", "2.11.2", "2.10.4")
    )
    .settings(SbtBuildInfo(): _*)
}