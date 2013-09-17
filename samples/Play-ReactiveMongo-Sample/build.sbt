import play.Project._

name := "Play2-ReactiveMongo-Sample"

version := "1.0-SNAPSHOT"

playScalaSettings

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.10.0-SNAPSHOT"
