name := "play2-mongodb-async"

organization := "play.modules.mongodb"

version := "0.1-SNAPSHOT"

resolvers ++= Seq(
//	"Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
//	"Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
	"mandubian-mvn snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots",
	"mandubian-mvn releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases",
	"Scala-Tools" at "https://oss.sonatype.org/content/groups/scala-tools/",
	"Scala-Tools-Snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "org.asyncmongo" %% "mongo-async-driver" % "0.1-SNAPSHOT",
  "play.api.data" %% "play2-data-resources" % "0.1-SNAPSHOT",
  "play" %% "play" % "2.1-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.7.1" % "test",
  "junit" % "junit" % "4.8" % "test"  
)

publishTo <<=  version { (v: String) => 
    val base = "../../workspace_mandubian/mandubian-mvn"
	if (v.trim.endsWith("SNAPSHOT")) 
		Some(Resolver.file("snapshots", new File(base + "/snapshots")))
	else Some(Resolver.file("releases", new File(base + "/releases")))
}

publishMavenStyle := true

publishArtifact in Test := false