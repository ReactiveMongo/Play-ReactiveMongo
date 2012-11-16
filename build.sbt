name := "play2-reactivemongo"

organization := "play.modules.reactivemongo"

scalaVersion := "2.10.0-RC1"

version := "0.1-SNAPSHOT"

resolvers += "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/" 

resolvers += "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/"

// resolvers += Resolver.file("local repository", file("/Users/sgo/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "Scala-Tools" at "https://oss.sonatype.org/content/groups/scala-tools/"

resolvers += "Scala-Tools-Snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "reactivemongo" %% "reactivemongo" % "0.1-SNAPSHOT",
  "play" %% "play" % "2.1-SNAPSHOT",
  "org.specs2" % "specs2_2.10.0-RC2" % "1.12.2" % "test",
  "junit" % "junit" % "4.8" % "test"
)

publishTo <<= version { (version: String) =>
  val localPublishRepo = "/Volumes/Data/code/repository"
  if(version.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
  else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
}

publishMavenStyle := true
