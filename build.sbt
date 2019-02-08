lazy val playReactiveMongo = Project("Play-ReactiveMongo", file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    makePublicallyAvailableOnBintray := true,
    majorVersion := 6,
    scalaVersion := "2.11.12",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases")
    ),
    crossScalaVersions := Seq("2.11.12")
  )
