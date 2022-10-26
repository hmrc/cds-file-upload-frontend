import sbt._

object Dependencies {

  val bootstrapPlayVersion = "7.1.0"
  val hmrcMongoVersion = "0.68.0"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "play-frontend-hmrc"         % "3.20.0-play-28",
    "uk.gov.hmrc"               %% "crypto"                     % "7.1.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "com.github.pureconfig"     %% "pureconfig"                 % "0.17.1"
  ).map(_.withSources)

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % "test",
    "org.mockito"            %% "mockito-scala"           % "1.17.12"            % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0"           % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.62.2"             % "test",
    "org.jsoup"              %  "jsoup"                   % "1.15.3"             % "test"
  ).map(moduleID => if (moduleID.name.contains("flexmark")) moduleID else moduleID.withSources)
}
