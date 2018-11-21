import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.core.PlayVersion.current

val appName = "cds-file-upload-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= Seq(
      "uk.gov.hmrc"             %% "govuk-template"           % "5.25.0-play-25",
      "uk.gov.hmrc"             %% "play-ui"                  % "7.25.0-play-25",
      "uk.gov.hmrc"             %% "bootstrap-play-25"        % "3.14.0",

      "org.scalatest"           %% "scalatest"                % "3.0.4"                 % "test",
      "org.jsoup"               %  "jsoup"                    % "1.10.2"                % "test",
      "com.typesafe.play"       %% "play-test"                % current                 % "test",
      "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test",
      "uk.gov.hmrc"             %% "service-integration-test" % "0.2.0"                 % "test",
      "org.scalatestplus.play"  %% "scalatestplus-play"       % "2.0.0"                 % "test"
    )
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
