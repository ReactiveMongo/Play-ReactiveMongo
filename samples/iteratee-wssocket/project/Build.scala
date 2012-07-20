import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "backbone-test"
    val appVersion      = "0.1-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "play.modules.mongodb" %% "play2-mongodb-async" % "0.1-SNAPSHOT"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += ("mandubian-mvn snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots")
    )

}
