import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.12.1-SNAPSHOT"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.reactivemongo",
    version := buildVersion,
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8"),
    scalacOptions in Compile ++= Seq(
      "-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-numeric-widen"),
    scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation",
      /*"-diagrams", */"-implicits", "-skip-packages", "samples") ++
      Opts.doc.title("ReactiveMongo Play plugin") ++
      Opts.doc.version(buildVersion),
    crossScalaVersions := Seq(scalaVersion.value),
    crossVersion := CrossVersion.binary,
    shellPrompt := ShellPrompt.buildShellPrompt,
    fork in Test := false,
    testOptions in Test += Tests.Cleanup(cl => {
      import scala.language.reflectiveCalls
      val c = cl.loadClass("Common$")
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]
      m.close()
    })
  ) ++ Publish.settings ++ Format.settings ++ Travis.settings ++ (
    Publish.mimaSettings ++ Findbugs.settings)
}

object Publish {
  import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
  import com.typesafe.tools.mima.plugin.MimaKeys.{
    binaryIssueFilters, previousArtifacts
  }
  import com.typesafe.tools.mima.core._, ProblemFilters._, Problem.ClassVersion

  val previousVersion = "0.11.0.play24"

  @inline def env(n: String): String = sys.env.getOrElse(n, n)

  val missingMethodInOld: ProblemFilter = {
    case mmp @ MissingMethodProblem(_) if (
      mmp.affectedVersion == ClassVersion.Old) => false

    case _ => true
  }

  val mimaSettings = mimaDefaultSettings ++ Seq(
    previousArtifacts := {
      if (crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
      } else {
        Set(organization.value % moduleName.value % previousVersion)
      }
    },
    binaryIssueFilters ++= Seq(missingMethodInOld))

  private val repoName = env("PUBLISH_REPO_NAME")
  private val repoUrl = env("PUBLISH_REPO_URL")
  val majorVer = "0.12"

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
      s"https://reactivemongo.github.io/Play-ReactiveMongo/$majorVer/api/")),
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

object Findbugs {
  import scala.xml.{ NodeSeq, XML }, XML.{ loadFile => loadXML }

  import de.johoop.findbugs4sbt.{ FindBugs, ReportType }, FindBugs.{
    findbugsExcludeFilters, findbugsReportPath, findbugsReportType,
    findbugsSettings
  }

  val settings = findbugsSettings ++ Seq(
    findbugsReportType := Some(ReportType.PlainHtml),
    findbugsReportPath := Some(target.value / "findbugs.html"),
    findbugsExcludeFilters := {
      val filters = {
        val f = baseDirectory.value / "project" / "findbugs-exclude-filters.xml"
        if (!f.exists) NodeSeq.Empty else loadXML(f).child
      }

      Some(
        <FindBugsFilter>${filters}</FindBugsFilter>
      )
    }
  )
}

object Format {
  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences)

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, false).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, false).
      setPreference(CompactStringConcatenation, false).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, false).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, false).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, true).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesAroundMultiImports, true).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {

  object devnull extends ProcessLogger {
    def info(s: => String) {}

    def error(s: => String) {}

    def buffer[T](f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract(state).currentProject.id
      s"$currProject:$currBranch:${BuildSettings.buildVersion}> "
    }
  }
}

object Travis {
  val travisEnv = taskKey[Unit]("Print Travis CI env")

  val travisSnapshotBranches =
    SettingKey[Seq[String]]("branches that can be published on sonatype")

  // TODO: Review | remove
  val travisCommand = Command.command("publishSnapshotsFromTravis") { state =>
    val extracted = Project extract state
    import extracted._
    import scala.util.Properties.isJavaAtLeast

    val thisRef = extracted.get(thisProjectRef)

    val isSnapshot = getOpt(version).exists(_.endsWith("SNAPSHOT"))
    val isTravisEnabled = sys.env.get("TRAVIS").exists(_ == "true")
    val isNotPR = sys.env.get("TRAVIS_PULL_REQUEST").exists(_ == "false")
    val isBranchAcceptable = sys.env.get("TRAVIS_BRANCH").exists(branch => getOpt(travisSnapshotBranches).exists(_.contains(branch)))
    val isJavaVersion = !isJavaAtLeast("1.7")

    if (isSnapshot && isTravisEnabled && isNotPR && isBranchAcceptable) {
      println(s"publishing $thisRef from travis...")

      val newState = append(
        Seq(
          publishTo := Some("Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"),
          credentials := Seq(Credentials(
            "Sonatype Nexus Repository Manager",
            "oss.sonatype.org",
            sys.env.getOrElse("SONATYPE_USER", throw new RuntimeException("no SONATYPE_USER defined")),
            sys.env.getOrElse("SONATYPE_PASSWORD", throw new RuntimeException("no SONATYPE_PASSWORD defined"))
          ))),
        state
      )

      runTask(publish in thisRef, newState)

      println(s"published $thisRef from travis")
    } else {
      println(s"not publishing $thisRef to Sonatype: isSnapshot=$isSnapshot, isTravisEnabled=$isTravisEnabled, isNotPR=$isNotPR, isBranchAcceptable=$isBranchAcceptable, javaVersionLessThen_1_7=$isJavaVersion")
    }

    state
  }

  val settings = Seq(
    Travis.travisSnapshotBranches := Seq("master"),
    commands += Travis.travisCommand,
    travisEnv in Test := { // test:travisEnv from SBT CLI
      val specs = List[(String, List[String])](
        "PLAY_VERSION" -> List("2.3.10", "2.5.9")
      )

      def matrix = specs.flatMap {
        case (key, values) => values.map(key -> _)
      }.combinations(specs.size).collect {
        case flags if (flags.map(_._1).toSet.size == specs.size) =>
          flags.sortBy(_._1).map { case (k, v) => s"$k=$v" }
      }.map { c => s"""  - ${c mkString " "}""" }

      println(s"""Travis CI env:\r\n${matrix.mkString("\r\n")}""")
    }
  )
}

object Play2ReactiveMongoBuild extends Build {
  import com.typesafe.tools.mima.core._, ProblemFilters._, Problem.ClassVersion
  import com.typesafe.tools.mima.plugin.MimaKeys.{
    binaryIssueFilters, previousArtifacts
  }

  import BuildSettings._

  val specsVersion = "3.8.2"
  val specs2Dependencies = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specsVersion % Test cross CrossVersion.binary)

  val PlayVersion = sys.env.get("PLAY_VERSION").getOrElse("2.5.8")

  lazy val reactivemongo = Project(
    "Play2-ReactiveMongo",
    file("."),
    settings = buildSettings ++ Seq(
      resolvers := Seq(
        "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype" at "http://oss.sonatype.org/content/groups/public/",
        "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
        "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      ),
      libraryDependencies ++= Seq(
        ("org.reactivemongo" %% "reactivemongo" % buildVersion cross CrossVersion.binary).
          exclude("com.typesafe.akka", "*"). // provided by Play
          exclude("com.typesafe.play", "*"),
        "org.reactivemongo" %% "reactivemongo-play-json" % buildVersion cross CrossVersion.binary,
        "com.typesafe.play" %% "play" % PlayVersion % "provided" cross CrossVersion.binary,
        "com.typesafe.play" %% "play-test" % PlayVersion % Test cross CrossVersion.binary,
        "junit" % "junit" % "4.12" % Test cross CrossVersion.Disabled,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.5" % Test
      ) ++ specs2Dependencies,
      binaryIssueFilters ++= {
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
    )
  )
}
