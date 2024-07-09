import play.sbt.routes.RoutesKeys
import sbt.Keys.{scalacOptions, *}
import sbt.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.DefaultBuildSettings.*

name := "cds-file-upload-frontend"
majorVersion := 0

PlayKeys.devSettings := Seq("play.server.http.port" -> "6793")

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(commonSettings: _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    // concatenate js
    Concat.groups := Seq(
      "javascripts/validation.min.js" -> group(Seq("javascripts/validation.js")),
      "javascripts/analytics.min.js" -> group(Seq("javascripts/analytics.js"))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat,uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("validation.min.js"),
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := Seq(
      (IntegrationTest / baseDirectory).value / "test/it",
      (Test / baseDirectory).value / "test/util"
    ),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false
  )
  .settings(scoverageSettings)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val commonSettings = Seq(
  scalaVersion := "2.13.12",
  scalacOptions ++= scalacFlags,
  libraryDependencies ++= Dependencies.compile ++ Dependencies.test
)

lazy val scalacFlags = Seq(
  "-deprecation",            // warn about use of deprecated APIs
  "-encoding", "UTF-8",      // source files are in UTF-8
  "-feature",                // warn about misused language features
  "-unchecked",              // warn about unchecked type parameters
  "-Ywarn-numeric-widen",
  "-Xfatal-warnings",        // warnings are fatal!!
  "-Wconf:cat=unused-imports&src=routes/.*:s",       // silent "unused import" warnings from Play routes
  "-Wconf:cat=unused-imports&src=twirl/.*:is",       // silent "unused import" warnings from Twirl templates
  "-Wconf:site=.*Module.*&cat=lint-byname-implicit:s",  // silent warnings from Pureconfig/Shapeless
  "-Wconf:cat=unused&src=.*routes.*:s" // silence private val defaultPrefix in class Routes is never used
)

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>",
    "Reverse.*",
    "metrics\\..*",
    "features\\..*",
    "filters.*;config.*",
    "views.html.components\\..*",
    "controllers.test\\..*",
    "test\\..*",
    ".*(BuildInfo|Routes|Options|TestingUtilitiesController).*",
    "logger.*\\(.*\\)"
  ).mkString(";"),
  coverageMinimumStmtTotal := 90, // Minimum threshold to be increased
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)
