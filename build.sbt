name := "play2-mongodb-async"

organization := "play.modules.mongodb"

version := "0.1-SNAPSHOT"

resolvers += "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/" 

resolvers += Resolver.file("local repository", file("/Users/pvo/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "Scala-Tools" at "https://oss.sonatype.org/content/groups/scala-tools/"

resolvers += "Scala-Tools-Snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.asyncmongo" %% "mongo-async-driver" % "0.1-SNAPSHOT",
  "play.api.data" %% "play2-data-resources" % "0.1-SNAPSHOT",
  "play" %% "play" % "2.1-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.7.1" % "test",
  "junit" % "junit" % "4.8" % "test"  
)