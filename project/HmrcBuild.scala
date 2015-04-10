import sbt._
import sbt.Keys._

object HmrcBuild extends Build {

  import uk.gov.hmrc.DefaultBuildSettings
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}

  val appVersion = "3.4.2-SNAPSHOT"

  val simpleReactiveMongoVersion = "2.6.1"

  lazy val pluginDependencies = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "provided",
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "test" classifier "tests",

    "com.typesafe.play" %% "play" % "2.3.8" % "provided",
    "com.typesafe.play" %% "play-test" % "2.3.8" % "test",

    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.pegdown" % "pegdown" % "1.5.0" % "test"
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
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      crossScalaVersions := Seq("2.11.6")
    )
    .settings(SbtBuildInfo(): _*)
}
