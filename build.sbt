import com.typesafe.tools.mima.core._, ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{
  mimaBinaryIssueFilters, mimaPreviousArtifacts
}

import BuildSettings._

val specsVersion = "4.2.0"
val specs2Dependencies = Seq(
  "specs2-core",
  "specs2-junit"
).map("org.specs2" %% _ % specsVersion % Test)

val playDependencies = Def.setting[Seq[ModuleID]] {
  val ver = playVer.value
  val base = Seq(
    "play" -> Provided,
    "play-test" -> Test
  )

  val baseDeps = base.map {
    case (name, scope) =>
      ("com.typesafe.play" %% name % ver % scope) cross CrossVersion.binary
  }

  if (!playVer.value.startsWith("2.6")) baseDeps
  else {
    val iterateesVer = "2.6.1"

    baseDeps ++ Seq(
      "com.typesafe.play" %% "play-iteratees" % iterateesVer,
      (("com.typesafe.play" %% "play-iteratees-reactive-streams" % iterateesVer).cross(CrossVersion.binary))
    )
  }
}

lazy val reactivemongo = Project("Play2-ReactiveMongo", file(".")).
  settings(buildSettings ++ Seq(
    resolvers := Seq(
      Resolver.sonatypeRepo("snapshots"),
      "Sonatype" at "http://oss.sonatype.org/content/groups/public/",
      "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
    ),
    libraryDependencies ++= Seq(
      ("org.reactivemongo" %% "reactivemongo" % (
        version in ThisBuild).value cross CrossVersion.binary).
        exclude("com.typesafe.akka", "*"). // provided by Play
        exclude("com.typesafe.play", "*"),
      "org.reactivemongo" %% "reactivemongo-play-json" % version.value cross CrossVersion.binary,
      "junit" % "junit" % "4.12" % Test,
      "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.5" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.1" % Test
    ) ++ playDependencies.value ++ specs2Dependencies,
    mimaBinaryIssueFilters ++= {
      import ProblemFilters.{ exclude => x }
      @inline def mmp(s: String) = x[MissingMethodProblem](s)
      @inline def imt(s: String) = x[IncompatibleMethTypeProblem](s)
      @inline def irt(s: String) = x[IncompatibleResultTypeProblem](s)
      @inline def mtp(s: String) = x[MissingTypesProblem](s)
      @inline def mcp(s: String) = x[MissingClassProblem](s)

      Seq(
        mtp("play.modules.reactivemongo.JSONFileToSave"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productElement"),
        irt("play.modules.reactivemongo.JSONFileToSave.pack"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productArity"),
        mmp("play.modules.reactivemongo.JSONFileToSave.canEqual"),
        irt("play.modules.reactivemongo.JSONFileToSave.filename"),
        mmp("play.modules.reactivemongo.JSONFileToSave.copy"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productIterator"),
        mmp("play.modules.reactivemongo.JSONFileToSave.productPrefix"),
        imt("play.modules.reactivemongo.JSONFileToSave.this"),
        mcp("play.modules.reactivemongo.ReactiveMongoPlugin$"),
        mtp("play.modules.reactivemongo.JSONFileToSave$"),
        mmp("play.modules.reactivemongo.JSONFileToSave.unapply"),
        mmp("play.modules.reactivemongo.JSONFileToSave.apply"),
        mcp("play.modules.reactivemongo.ReactiveMongoHelper$"),
        mmp("play.modules.reactivemongo.MongoController.gridFSBodyParser"),
        mmp("play.modules.reactivemongo.MongoController.gridFSBodyParser"),
        mcp("play.modules.reactivemongo.ReactiveMongoPlugin"),
        mcp("play.modules.reactivemongo.ReactiveMongoHelper"),
        irt("play.modules.reactivemongo.json.LowerImplicitBSONHandlers.BSONValueWrites"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONArrayFormat.this"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONDocumentFormat.this"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONDocumentFormat.this"),
        mmp("play.modules.reactivemongo.json.BSONFormats#BSONArrayFormat.this"),
        ProblemFilters.exclude[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.reads"),
        ProblemFilters.exclude[UpdateForwarderBodyProblem]("play.modules.reactivemongo.json.BSONFormats#PartialFormat.writes"),
        ProblemFilters.exclude[IncompatibleTemplateDefProblem]("play.modules.reactivemongo.json.BSONFormats"),
        irt("play.modules.reactivemongo.json.JSONSerializationPack.IdentityWriter"),
        irt("play.modules.reactivemongo.json.JSONSerializationPack.IdentityReader"),
        irt("play.modules.reactivemongo.json.ImplicitBSONHandlers.BSONValueWrites"),
        irt("play.modules.reactivemongo.json.collection.JSONBatchCommands.pack"),
        mtp("play.modules.reactivemongo.json.collection.JSONQueryBuilder$"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.apply"),
        irt("play.modules.reactivemongo.json.collection.JSONQueryBuilder.pack"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.copy"),
        mmp("play.modules.reactivemongo.json.collection.JSONQueryBuilder.this"),
        irt("play.modules.reactivemongo.json.collection.JSONCollection.pack")
      )
    }
  ))
