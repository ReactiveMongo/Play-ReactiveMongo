import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.12-RC3"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.reactivemongo",
    version := s"$buildVersion-play24",
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8"),
    scalacOptions in Compile ++= Seq(
      "-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-numeric-widen"),
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
    Publish.mimaSettings)
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
  val travisSnapshotBranches =
    SettingKey[Seq[String]]("branches that can be published on sonatype")

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
    commands += Travis.travisCommand)
  
}

object Play2ReactiveMongoBuild extends Build {
  import BuildSettings._

  val specsVersion = "3.8.2"
  val specs2Dependencies = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specsVersion % Test cross CrossVersion.binary)

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
          exclude("com.typesafe.akka", "*").
          exclude("com.typesafe.play", "*"),
        "org.reactivemongo" %% "reactivemongo-play-json" % s"$buildVersion-play24" cross CrossVersion.binary,
        "com.typesafe.play" %% "play" % "2.4.8" % "provided" cross CrossVersion.binary,
        "com.typesafe.play" %% "play-test" % "2.4.8" % Test cross CrossVersion.binary,
        "junit" % "junit" % "4.12" % Test cross CrossVersion.Disabled,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.5" % Test
      ) ++ specs2Dependencies
    )
  )
}
