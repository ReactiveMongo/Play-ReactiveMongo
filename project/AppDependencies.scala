import sbt._

object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc"       %% "simple-reactivemongo" % "6.1.0",
    "com.typesafe.play" %% "play"                 % "2.5.12" % Provided
  )

  val test = Seq(
    "com.typesafe.play"  %% "play-test"   % "2.5.12" % Test,
    "com.typesafe.play"  %% "play-specs2" % "2.5.12" % Test,
    "org.scalatest"      %% "scalatest"   % "2.2.4"  % Test,
    "org.pegdown"        % "pegdown"      % "1.5.0"  % Test
  )
}
