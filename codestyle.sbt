ThisBuild / scalafmtOnCompile := true

// Scalafix
inThisBuild(
  List(
    // scalaVersion := "2.13.3",
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
