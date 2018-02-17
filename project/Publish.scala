import sbt._
import sbt.Keys._

import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{
  binaryIssueFilters, previousArtifacts
}
import com.typesafe.tools.mima.core._, ProblemFilters._, Problem.ClassVersion

object Publish {
  val previousVersion = "0.11.0.play24"

  @inline def env(n: String): String = sys.env.getOrElse(n, n)

  val missingMethodInOld: ProblemFilter = {
    case mmp @ MissingMethodProblem(_) if (
      mmp.affectedVersion == ClassVersion.Old) => false

    case _ => true
  }

  val mimaSettings = mimaDefaultSettings ++ Seq(
    previousArtifacts := {
      if (scalaVersion.value.startsWith("2.12.") && crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % "0.12.7-play26")
      } else if (crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
      } else {
        Set(organization.value % moduleName.value % previousVersion)
      }
    },
    binaryIssueFilters ++= Seq(
      missingMethodInOld,
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.JSONSerializationPack$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.JSONSerializationPack"),
      ProblemFilters.exclude[MissingMethodProblem]("play.modules.reactivemongo.json.collection.JSONCollection.sister"),
      ProblemFilters.exclude[FinalMethodProblem]("play.modules.reactivemongo.json.collection.JSONCollection.fullCollectionName"),
      ProblemFilters.exclude[MissingMethodProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands.LastErrorReader"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.modules.reactivemongo.json.collection.JSONQueryBuilder.merge"),
      ProblemFilters.exclude[MissingMethodProblem]("play.modules.reactivemongo.json.collection.JSONQueryBuilder.cursor"),
      ProblemFilters.exclude[MissingTypesProblem]("play.modules.reactivemongo.MongoController"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.modules.reactivemongo.json.commands.JSONAggregationFramework.PipelineOperator"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.insert"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregatorContext")
    ))

  private val repoName = env("PUBLISH_REPO_NAME")
  private val repoUrl = env("PUBLISH_REPO_URL")

  lazy val settings = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := Some(repoUrl).map(repoName at _),
    credentials += Credentials(repoName, env("PUBLISH_REPO_ID"),
      env("PUBLISH_USER"), env("PUBLISH_PASS")),
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2.0" ->
      url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("http://reactivemongo.org")),
    autoAPIMappings := true,
    apiURL := Some(url(
      s"https://reactivemongo.github.io/Play-ReactiveMongo/${Release.major.value}/api/")),
    pomExtra :=
      <scm>
        <url>git://github.com/ReactiveMongo/Play-ReactiveMongo.git</url>
        <connection>scm:git://github.com/ReactiveMongo/Play-ReactiveMongo.git</connection>
      </scm>
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
      </developers>
  )
}
