import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters, mimaPreviousArtifacts
}

val specsVersion = "4.3.2"
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

  if (ver.startsWith("2.5")) baseDeps
  else {
    val iterateesVer = "2.6.1"

    baseDeps ++ Seq(
      "com.typesafe.play" %% "play-iteratees" % iterateesVer,
      (("com.typesafe.play" %% "play-iteratees-reactive-streams" % iterateesVer).cross(CrossVersion.binary))
    )
  }
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
      val silencerVer = "1.2-SNAPSHOT"

      def silencer = Seq(
        compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVer),
        "com.github.ghik" %% "silencer-lib" % silencerVer % Provided)

      Seq(("org.reactivemongo" %% "reactivemongo" % (
        version in ThisBuild).value cross CrossVersion.binary).
        exclude("com.typesafe.akka", "*"). // provided by Play
        exclude("com.typesafe.play", "*"),
        "org.reactivemongo" %% "reactivemongo-play-json" % version.value cross CrossVersion.binary,
        "junit" % "junit" % "4.12" % Test,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.5" % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.1" % Test
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
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.modules.reactivemongo.MongoController.gridFSBodyParser"),
        ProblemFilters.exclude[ReversedMissingMethodProblem]("play.modules.reactivemongo.ReactiveMongoApiComponents.executionContext"),
        imt("play.modules.reactivemongo.ReactiveMongoProvider.this"),
        ProblemFilters.exclude[IncompatibleTemplateDefProblem]("play.modules.reactivemongo.json.BSONFormats"),
        ProblemFilters.exclude[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.reads"),
        ProblemFilters.exclude[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.writes"),
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
