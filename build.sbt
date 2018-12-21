import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.core.PlayVersion.current

val appName = "cds-file-upload-frontend"
val jacksonVersion = "2.9.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= Seq(
      "uk.gov.hmrc"                      %% "govuk-template"           % "5.25.0-play-25",
      "uk.gov.hmrc"                      %% "play-ui"                  % "7.25.0-play-25",
      "uk.gov.hmrc"                      %% "bootstrap-play-25"        % "3.14.0",
      "com.github.pureconfig"            %% "pureconfig"               % "0.9.2",
      "com.fasterxml.jackson.core"       %  "jackson-core"             % jacksonVersion,
      "com.fasterxml.jackson.core"       %  "jackson-databind"         % jacksonVersion,
      "com.fasterxml.jackson.core"       %  "jackson-annotations"      % jacksonVersion,
      "com.fasterxml.jackson.dataformat" %  "jackson-dataformat-xml"   % jacksonVersion,
      "com.fasterxml.jackson.module"     %% "jackson-module-scala"     % jacksonVersion,

      "org.scalatest"              %% "scalatest"                 % "3.0.4"  % "test",
      "org.jsoup"                  %  "jsoup"                     % "1.10.2" % "test",
      "com.typesafe.play"          %% "play-test"                 % current  % "test",
      "org.pegdown"                %  "pegdown"                   % "1.6.0"  % "test",
      "uk.gov.hmrc"                %% "service-integration-test"  % "0.2.0"  % "test",
      "org.scalatestplus.play"     %% "scalatestplus-play"        % "2.0.0"  % "test",
      "org.mockito"                %  "mockito-core"              % "2.13.0" % "test",
      "org.scalacheck"             %% "scalacheck"                % "1.14.0" % "test"

    )
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
