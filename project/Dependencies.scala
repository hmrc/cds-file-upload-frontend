import sbt._

object Dependencies {

  val bootstrapPlayVersion = "8.3.0"
  val frontendPlayVersion = "8.5.0"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-30"     % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "play-frontend-hmrc-play-30"     % frontendPlayVersion,
    "uk.gov.hmrc"               %% "play-partials-play-30"          % "9.1.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-30"             % "1.7.0",
    "com.github.pureconfig"     %% "pureconfig"                     % "0.17.4"
  ).map(_.withSources)

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapPlayVersion % "test, it",
    "org.mockito"            %% "mockito-scala"           % "1.17.29"            % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0"           % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.64.6"             % "test",
    "org.jsoup"              %  "jsoup"                   % "1.15.4"             % "test"
  ).map(moduleID => if (moduleID.name.contains("flexmark")) moduleID else moduleID.withSources)
}
