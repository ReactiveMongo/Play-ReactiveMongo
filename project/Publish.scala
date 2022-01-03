import sbt._
import sbt.Keys._

import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters,
  mimaPreviousArtifacts
}
import com.typesafe.tools.mima.core._, ProblemFilters._

object Publish {
  val previousVersion = "0.12.7-play26"

  @inline def env(n: String): String = sys.env.getOrElse(n, n)

  val mimaSettings = Seq(mimaPreviousArtifacts := {
    val v = scalaBinaryVersion.value

    if (v == "3" || v == "2.13") {
      Set.empty[ModuleID]
    } else if (v == "2.12" && crossPaths.value) {
      Set(
        organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % "0.12.7-play26"
      )
    } else if (crossPaths.value) {
      Set(
        organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion
      )
    } else {
      Set(organization.value % moduleName.value % previousVersion)
    }
  })

  private val repoName = env("PUBLISH_REPO_NAME")
  private val repoUrl = env("PUBLISH_REPO_URL")

  lazy val settings = Seq(
    Compile / doc / scalacOptions ++= {
      if (scalaBinaryVersion.value startsWith "2.") {
        Seq( /*"-diagrams", */ "-implicits", "-skip-packages", "samples")
      } else {
        Seq("-skip-by-id:samples")
      }
    },
    Compile / doc / scalacOptions ++= Opts.doc.title(
      "ReactiveMongo Play plugin"
    ) ++ Opts.doc.version(Release.major.value),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    publishTo := Some(repoUrl).map(repoName at _),
    credentials += Credentials(
      repoName,
      env("PUBLISH_REPO_ID"),
      env("PUBLISH_USER"),
      env("PUBLISH_PASS")
    ),
    pomIncludeRepository := { _ => false },
    licenses := Seq(
      "Apache 2.0" ->
        url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("http://reactivemongo.org")),
    autoAPIMappings := true,
    apiURL := Some(
      url(s"https://reactivemongo.github.io/Play-ReactiveMongo/${Release.major.value}/api/")
    ),
    pomExtra :=
      <developers>
        <developer>
          <id>sgodbillon</id>
          <name>Stephane Godbillon</name>
          <url>http://stephane.godbillon.com</url>
        </developer>
        <developer>
          <id>mandubian</id>
          <name>Pascal Voitot</name>
          <url>http://mandubian.com</url>
        </developer>
        <developer>
          <id>cchantep</id>
          <name>Cedric Chantepie</name>
          <url>github.com/cchantep/</url>
        </developer>
      </developers>
  )
}
