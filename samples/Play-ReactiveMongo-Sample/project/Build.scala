import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Play2-ReactiveMongo-Sample"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "play.modules.reactivemongo" % "play2-reactivemongo_2.10.0" % "0.1-SNAPSHOT",
      "org.slf4j" % "slf4j-api" % "1.6.6"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/"
    )
}