resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

resolvers ++= Seq(
  "jgit-repo" at "http://download.eclipse.org/jgit/maven",
  Resolver.url("jsuereth-repo", url(
    "https://dl.bintray.com/jsuereth/sbt-plugins/"))(Resolver.ivyStylePatterns))

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.8")
