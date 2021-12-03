import sbt._

object Dependencies {

  val bootstrapPlayVersion = "5.16.0"
  val hmrcMongoVersion = "0.56.0"
  val httpComponentsVersion = "4.5.13"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "govuk-template"             % "5.69.0-play-28",
    "uk.gov.hmrc"               %% "play-frontend-hmrc"         % "0.94.0-play-28",
    "uk.gov.hmrc"               %% "crypto"                     % "5.6.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "com.github.pureconfig"     %% "pureconfig"                 % "0.15.0",
    "com.typesafe.play"         %% "play-json"                  % "2.9.2",
    "joda-time"                 %  "joda-time"                  % "2.10.13",
    "org.apache.httpcomponents" %  "httpclient"                 % httpComponentsVersion,
    "org.apache.httpcomponents" %  "httpmime"                   % httpComponentsVersion
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % "test",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion     % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"  % "test",
    "org.jsoup"              %  "jsoup"                   % "1.14.2"  % "test",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.9.0" % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.9.0" % "test",
    "com.github.tomakehurst" %  "wiremock-standalone"     % "2.27.2"  % "test"
  )
}
