import play.core.PlayVersion.current
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

name := "cds-file-upload-frontend"
majorVersion := 0

PlayKeys.devSettings := Seq("play.server.http.port" -> "6793")

resolvers += Resolver.bintrayRepo("wolfendale", "maven")

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(publishingSettings: _*)
  .settings(resolvers += Resolver.jcenterRepo)
 

val jacksonVersion = "2.9.8"
val httpComponentsVersion = "4.5.8"

val compileDependencies = Seq(
  "uk.gov.hmrc" %% "govuk-template" % "5.25.0-play-25",
  "uk.gov.hmrc" %% "play-ui" % "7.33.0-play-25",
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.10.0",
  "uk.gov.hmrc" %% "auth-client" % "2.17.0-play-25",
  "com.github.pureconfig" %% "pureconfig" % "0.9.2",
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "org.apache.httpcomponents"        %  "httpclient"               % httpComponentsVersion,
  "org.apache.httpcomponents"        %  "httpmime"                 % httpComponentsVersion,
  "io.megl" %% "play-json-extra" % "2.4.3",
  "uk.gov.hmrc" %% "http-caching-client" % "8.2.0",
  "uk.gov.hmrc" %% "play-whitelist-filter" % "2.0.0",
  "uk.gov.hmrc" %% "crypto" % "5.3.0",
  "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.19.0-play-25"
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.jsoup" % "jsoup" % "1.11.3" % "test",
  "com.typesafe.play" %% "play-test" % current % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test",
  "org.mockito" % "mockito-core" % "2.27.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "wolfendale" %% "scalacheck-gen-regexp" % "0.1.1" % "test",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.22.0" % "test"
)

libraryDependencies ++= compileDependencies ++ testDependencies
