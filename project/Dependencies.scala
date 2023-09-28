import sbt._

object Dependencies {

  val bootstrapPlayVersion = "7.22.0"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "play-frontend-hmrc"         % "7.20.0-play-28",
    "uk.gov.hmrc"               %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"         % "1.3.0",
    "com.github.pureconfig"     %% "pureconfig"                 % "0.17.4"
  ).map(_.withSources)

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % "test, it",
    "org.mockito"            %% "mockito-scala"           % "1.17.12"            % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0"           % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.64.6"             % "test",
    "org.jsoup"              %  "jsoup"                   % "1.15.4"             % "test"
  ).map(moduleID => if (moduleID.name.contains("flexmark")) moduleID else moduleID.withSources)
}
