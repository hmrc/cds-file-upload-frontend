import sbt._

object Dependencies {

  val bootstrapPlayVersion = "5.16.0"
  val hmrcMongoVersion = "0.56.0"
  val httpComponentsVersion = "4.5.13"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "govuk-template"             % "5.69.0-play-28",
    "uk.gov.hmrc"               %% "play-frontend-hmrc"         % "3.5.0-play-28",
    "uk.gov.hmrc"               %% "crypto"                     % "5.6.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "com.github.pureconfig"     %% "pureconfig"                 % "0.17.1",
    // We still need this artifact due to external deps, e.g. Auth
    "joda-time"                 %  "joda-time"                  % "2.10.13",
    "org.apache.httpcomponents" %  "httpclient"                 % httpComponentsVersion,
    "org.apache.httpcomponents" %  "httpmime"                   % httpComponentsVersion
  ).map(_.withSources)

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"  % "test",
    "org.jsoup"              %  "jsoup"                   % "1.14.2"  % "test",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.9.0" % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.9.0" % "test",
    "com.github.tomakehurst" %  "wiremock-standalone"     % "2.27.2"  % "test"
  ).map(moduleID => if (moduleID.name.contains("flexmark")) moduleID else moduleID.withSources)
}
