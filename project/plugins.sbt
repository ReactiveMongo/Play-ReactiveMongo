resolvers ++= Seq(
  /*Resolver.url(
    "jsuereth-repo",
    url("https://dl.bintray.com/jsuereth/sbt-plugins/")
  )(Resolver.ivyStylePatterns),*/
  "Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.5")

addSbtPlugin("cchantep" % "sbt-hl-compiler" % "0.12")

addSbtPlugin("cchantep" % "sbt-scaladoc-compiler" % "0.8")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.5.0")

addDependencyTreePlugin

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")
