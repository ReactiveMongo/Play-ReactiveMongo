import sbt._
import sbt.Keys._

import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters, mimaPreviousArtifacts
}
import com.typesafe.tools.mima.core._, ProblemFilters._

object Publish {
  val previousVersion = "0.12.1-play24"

  @inline def env(n: String): String = sys.env.getOrElse(n, n)

  val mimaSettings = mimaDefaultSettings ++ Seq(
    mimaPreviousArtifacts := {
      if (scalaVersion.value.startsWith("2.12.") && crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % "0.12.7-play26")
      } else if (crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
      } else {
        Set(organization.value % moduleName.value % previousVersion)
      }
    },
    mimaBinaryIssueFilters ++= Seq(
      //missingMethodInOld,
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.ReactiveMongoApi.db"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.db"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.parseConf"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.MongoController.db"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.LowerImplicitBSONHandlers"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.Writers"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.package"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.package$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.Writers"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.Writers$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationFramework"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.DealingWithGenericCommandErrorsReader"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONCommandError"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.CommonImplicits"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationFramework$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.CommonImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyCommand"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.DefaultJSONCommandError"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.CommonImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.DefaultJSONCommandError$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONCollection$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JsCursorImpl"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JsCursor$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONQueryBuilder$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JsFlattenedCursor"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONQueryBuilder"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONCollection"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.package"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.package$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JsCursor"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.package$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.package$"),
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
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.insert"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregatorContext"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$BSONArrayFormat$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$BSONDocumentFormat$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$PartialFormat$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$BSONDocumentFormat"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$BSONArrayFormat"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$PartialFormat"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.Writers$JsPathMongo"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectDocumentWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.ImplicitBSONHandlers$BSONDocumentWrites$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.Writers$JsPathMongo$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.package$JSONCollectionProducer$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$InsertWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$CountWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteConcernErrorReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$DistinctWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.BSONFormats$PartialFormat$apply"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$AggregateWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.CommonImplicits$UnitBoxReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$FindAndModifyWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$AggregationResultReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$FindAndModifyResultReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$DefaultWriteResultReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JsCursor$cursorFlattener$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONDeleteCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteConcernWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$LastErrorReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONInsertCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONCountCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$DeleteElementWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$DistinctResultReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateElementWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpsertedReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONUpdateCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteErrorReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$CountResultReader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONDistinctCommand$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$HintWriter$"),
      ProblemFilters.exclude[MissingClassProblem]("play.modules.reactivemongo.json.collection.JSONBatchCommands$DeleteWriter$")
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
