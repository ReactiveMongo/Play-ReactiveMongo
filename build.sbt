import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters, mimaPreviousArtifacts
}

val specsVersion = "4.6.0"
val specs2Dependencies = Seq("specs2-core", "specs2-junit").
  map("org.specs2" %% _ % specsVersion % Test)

val playDependencies = Def.setting[Seq[ModuleID]] {
  val ver = Common.playVer.value
  val base = Seq(
    "play" -> Provided,
    "play-test" -> Test
  )

  val baseDeps = base.map {
    case (name, scope) =>
      ("com.typesafe.play" %% name % ver % scope) cross CrossVersion.binary
  }

  baseDeps
}

lazy val reactivemongo = Project("Play2-ReactiveMongo", file(".")).
  settings(Common.settings ++ Seq(
    resolvers := Seq(
      Resolver.sonatypeRepo("snapshots"),
      "Sonatype" at "http://oss.sonatype.org/content/groups/public/",
      "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
      "Tatami Snapshots".at(
        "https://raw.github.com/cchantep/tatami/master/snapshots")
    ),
    libraryDependencies ++= {
      val silencerVer = "1.4.1"

      def silencer = Seq(
        compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVer),
        "com.github.ghik" %% "silencer-lib" % silencerVer % Provided)

      Seq(("org.reactivemongo" %% "reactivemongo" % (
        version in ThisBuild).value cross CrossVersion.binary).
        exclude("com.typesafe.akka", "*"). // provided by Play
        exclude("com.typesafe.play", "*"),
        "org.reactivemongo" %% "reactivemongo-play-json" % version.value cross CrossVersion.binary,
        "org.reactivemongo" %% "reactivemongo-akkastream" % (
          version in ThisBuild).value cross CrossVersion.binary,
        "junit" % "junit" % "4.12" % Test,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.5" % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
      ) ++ playDependencies.value ++ specs2Dependencies ++ silencer
    },
    mimaBinaryIssueFilters ++= {
      import ProblemFilters.{ exclude => x }
      @inline def mmp(s: String) = x[MissingMethodProblem](s)
      @inline def imt(s: String) = x[IncompatibleMethTypeProblem](s)
      @inline def irt(s: String) = x[IncompatibleResultTypeProblem](s)
      @inline def mtp(s: String) = x[MissingTypesProblem](s)
      @inline def mcp(s: String) = x[MissingClassProblem](s)

      Seq(
        //private
        mcp("play.modules.reactivemongo.Streams$"),
        mcp("play.modules.reactivemongo.Streams"),
        //missingMethodInOld,
        ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.ReactiveMongoApi.db"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.db"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.DefaultReactiveMongoApi.parseConf"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("play.modules.reactivemongo.MongoController.db"),
        mcp("play.modules.reactivemongo.json.LowerImplicitBSONHandlers"),
        mcp("play.modules.reactivemongo.json.BSONFormats$"),
        mcp("play.modules.reactivemongo.json.Writers"),
        mcp("play.modules.reactivemongo.json.package"),
        mcp("play.modules.reactivemongo.json.package$"),
        mcp("play.modules.reactivemongo.json.BSONFormats"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers"),
        mcp("play.modules.reactivemongo.json.Writers"),
        mcp("play.modules.reactivemongo.json.Writers$"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers$"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationFramework"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits"),
        mcp("play.modules.reactivemongo.json.commands.DealingWithGenericCommandErrorsReader"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
        mcp("play.modules.reactivemongo.json.commands.JSONCommandError"),
        mcp("play.modules.reactivemongo.json.commands.CommonImplicits"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationFramework$"),
        mcp("play.modules.reactivemongo.json.commands.CommonImplicits$"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyCommand"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
        mcp("play.modules.reactivemongo.json.commands.DefaultJSONCommandError"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits"),
        mcp("play.modules.reactivemongo.json.commands.CommonImplicits$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyCommand$"),
        mcp("play.modules.reactivemongo.json.commands.DefaultJSONCommandError$"),
        mcp("play.modules.reactivemongo.json.collection.JSONCollection$"),
        mcp("play.modules.reactivemongo.json.collection.JsCursorImpl"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$"),
        mcp("play.modules.reactivemongo.json.collection.JsCursor$"),
        mcp("play.modules.reactivemongo.json.collection.JSONQueryBuilder$"),
        mcp("play.modules.reactivemongo.json.collection.JsFlattenedCursor"),
        mcp("play.modules.reactivemongo.json.collection.JSONQueryBuilder"),
        mcp("play.modules.reactivemongo.json.collection.JSONCollection"),
        mcp("play.modules.reactivemongo.json.collection.package"),
        mcp("play.modules.reactivemongo.json.collection.package$"),
        mcp("play.modules.reactivemongo.json.collection.JsCursor"),
        mcp("play.modules.reactivemongo.json.collection.package$"),
        mcp("play.modules.reactivemongo.json.collection.package$"),
        mcp("play.modules.reactivemongo.json.JSONSerializationPack$"),
        mcp("play.modules.reactivemongo.json.JSONSerializationPack"),
        mmp("play.modules.reactivemongo.json.collection.JSONCollection.sister"),
        ProblemFilters.exclude[FinalMethodProblem]("play.modules.reactivemongo.json.collection.JSONCollection.fullCollectionName"),
        mmp("play.modules.reactivemongo.json.collection.JSONBatchCommands.LastErrorReader"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.modules.reactivemongo.json.collection.JSONQueryBuilder.merge"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.cursor"),
        ProblemFilters.exclude[MissingTypesProblem]("play.modules.reactivemongo.MongoController"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.modules.reactivemongo.json.commands.JSONAggregationFramework.PipelineOperator"),
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate"),
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregate1"),
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.insert"),
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.json.collection.JSONCollection.aggregatorContext"),
        mcp("play.modules.reactivemongo.json.BSONFormats$BSONArrayFormat$"),
        mcp("play.modules.reactivemongo.json.BSONFormats$BSONDocumentFormat$"),
        mcp("play.modules.reactivemongo.json.BSONFormats$PartialFormat$"),
        mcp("play.modules.reactivemongo.json.BSONFormats$BSONDocumentFormat"),
        mcp("play.modules.reactivemongo.json.BSONFormats$BSONArrayFormat"),
        mcp("play.modules.reactivemongo.json.BSONFormats$PartialFormat"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectReader$"),
        mcp("play.modules.reactivemongo.json.Writers$JsPathMongo"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectDocumentWriter$"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers$JsObjectWriter$"),
        mcp("play.modules.reactivemongo.json.ImplicitBSONHandlers$BSONDocumentWrites$"),
        mcp("play.modules.reactivemongo.json.Writers$JsPathMongo$"),
        mcp("play.modules.reactivemongo.json.collection.package$JSONCollectionProducer$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$InsertWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$CountWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteConcernErrorReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$DistinctWriter$"),
        mcp("play.modules.reactivemongo.json.BSONFormats$PartialFormat$apply"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$AggregateWriter$"),
        mcp("play.modules.reactivemongo.json.commands.CommonImplicits$UnitBoxReader$"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$FindAndModifyWriter$"),
        mcp("play.modules.reactivemongo.json.commands.JSONAggregationImplicits$AggregationResultReader$"),
        mcp("play.modules.reactivemongo.json.commands.JSONFindAndModifyImplicits$FindAndModifyResultReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$DefaultWriteResultReader$"),
        mcp("play.modules.reactivemongo.json.collection.JsCursor$cursorFlattener$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONDeleteCommand$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteConcernWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$LastErrorReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONInsertCommand$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONCountCommand$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$DeleteElementWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$DistinctResultReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpdateElementWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$UpsertedReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONUpdateCommand$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$WriteErrorReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$CountResultReader$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$JSONDistinctCommand$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$HintWriter$"),
        mcp("play.modules.reactivemongo.json.collection.JSONBatchCommands$DeleteWriter$"),
        x[ReversedMissingMethodProblem]( // protected
          "play.modules.reactivemongo.ReactiveMongoApiComponents.ec"),
        x[ReversedMissingMethodProblem](
          "play.modules.reactivemongo.MongoController.serve"),
        x[DirectMissingMethodProblem](
          "play.modules.reactivemongo.MongoController.serve"),
        imt("play.modules.reactivemongo.MongoController.serve"),
        //
        x[IncompatibleMethTypeProblem]("play.modules.reactivemongo.MongoController.gridFSBodyParser"),
        x[ReversedMissingMethodProblem]("play.modules.reactivemongo.ReactiveMongoApiComponents.executionContext"),
        imt("play.modules.reactivemongo.ReactiveMongoProvider.this"),
        x[IncompatibleTemplateDefProblem]("play.modules.reactivemongo.json.BSONFormats"),
        x[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.reads"),
        x[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.writes"),
        imt("play.modules.reactivemongo.JSONFileToSave.this"),
        irt("play.modules.reactivemongo.JSONFileToSave.filename"),
        irt("play.modules.reactivemongo.JSONFileToSave.pack"),
        irt("play.modules.reactivemongo.json.ImplicitBSONHandlers.BSONValueWrites"),
        irt("play.modules.reactivemongo.json.JSONSerializationPack.IdentityReader"),
        irt("play.modules.reactivemongo.json.JSONSerializationPack.IdentityWriter"),
        irt("play.modules.reactivemongo.json.LowerImplicitBSONHandlers.BSONValueWrites"),
        irt("play.modules.reactivemongo.json.collection.JSONBatchCommands.pack"),
        irt("play.modules.reactivemongo.json.collection.JSONCollection.pack"),
        irt("play.modules.reactivemongo.json.collection.JSONQueryBuilder.pack"),
        mcp("play.modules.reactivemongo.ReactiveMongoHelper"),
        mcp("play.modules.reactivemongo.ReactiveMongoHelper$"),
        mcp("play.modules.reactivemongo.ReactiveMongoPlugin"),
        mcp("play.modules.reactivemongo.ReactiveMongoPlugin$"),
        mmp("play.modules.reactivemongo.JSONFileToSave.apply"),
        mmp("play.modules.reactivemongo.JSONFileToSave.canEqual"),
        mmp("play.modules.reactivemongo.JSONFileToSave.copy"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productArity"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productElement"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productIterator"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productPrefix"),
        mmp("play.modules.reactivemongo.JSONFileToSave.unapply"),
        mmp("play.modules.reactivemongo.MongoController.gridFSBodyParser"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONArrayFormat.this"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONDocumentFormat.this"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.apply"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.copy"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.this"),
        mtp("play.modules.reactivemongo.JSONFileToSave"),
        mtp("play.modules.reactivemongo.JSONFileToSave$"),
        mtp("play.modules.reactivemongo.json.collection.JSONQueryBuilder$")
      )
    }
  ))
