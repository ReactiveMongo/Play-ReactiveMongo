import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Play2-ReactiveMongo-Sample"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.reactivemongo" %% "play2-reactivemongo" % "0.8"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
    )
}