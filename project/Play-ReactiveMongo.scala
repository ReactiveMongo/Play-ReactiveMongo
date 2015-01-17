import sbt._
import sbt.Keys._

object PlayReactiveMongoBuild extends Build {

  import uk.gov.hmrc.DefaultBuildSettings
  import DefaultBuildSettings._
  import BuildDependencies._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}

  lazy val pluginName = "Play-ReactiveMongo"
  val pluginVersion = "3.2.0"

  val simpleReactiveMongoVersion = "2.1.2"

  lazy val pluginDependencies = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "provided",
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion % "test" classifier "tests",

    "com.typesafe.play" %% "play" % "[2.2.1,2.3.7]" % "provided",
    "com.typesafe.play" %% "play-test" % "[2.2.1,2.3.7]" % "test",

    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test"
  )

  lazy val playReactiveMongo = Project(pluginName, file("."))
    .settings(version := pluginVersion)
    .settings(scalaSettings : _*)
    .settings(defaultSettings() : _*)
    .settings(
      targetJvm := "jvm-1.7",
      shellPrompt := ShellPrompt(pluginVersion),
      libraryDependencies ++= pluginDependencies,
      resolvers := Seq(
        Opts.resolver.sonatypeReleases,
        Opts.resolver.sonatypeSnapshots,
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/",
        "typesafe-snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
      ),
      crossScalaVersions := Seq("2.11.5", "2.11.2", "2.10.4")
    )
    .settings(SbtBuildInfo(): _*)
    .settings(SonatypeBuild(): _*)

}

object SonatypeBuild {

  import xerial.sbt.Sonatype._

  def apply() = {
    sonatypeSettings ++ Seq(
      pomExtra := (<url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git@github.com:hmrc/Play-ReactiveMongo.git</connection>
          <developerConnection>scm:git@github.com:hmrc/Play-ReactiveMongo.git</developerConnection>
          <url>git@github.com:hmrc/Play-ReactiveMongo.git</url>
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
          <developer>
            <id>xnejp03</id>
            <name>Petr Nejedly</name>
            <url>http://www.equalexperts.com</url>
          </developer>
          <developer>
            <id>duncancrawford</id>
            <name>Duncan Crawford</name>
            <url>http://www.equalexperts.com</url>
          </developer>
        </developers>)
    )
  }
}