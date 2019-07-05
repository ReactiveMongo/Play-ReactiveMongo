resolvers ++= Seq(
  "jgit-repo" at "http://download.eclipse.org/jgit/maven",
  Resolver.url("jsuereth-repo", url(
    "https://dl.bintray.com/jsuereth/sbt-plugins/"))(Resolver.ivyStylePatterns),
  "Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")

addSbtPlugin("cchantep" % "sbt-hl-compiler" % "0.7")

addSbtPlugin("com.github.sbt" % "sbt-findbugs" % "2.0.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
