resolvers ++= Seq(
  Resolver.url(
    "jsuereth-repo",
    url("https://dl.bintray.com/jsuereth/sbt-plugins/")
  )(Resolver.ivyStylePatterns),
  "Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.0")

addSbtPlugin("cchantep" % "sbt-hl-compiler" % "0.8")

addSbtPlugin("cchantep" % "sbt-scaladoc-compiler" % "0.3")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.0")
