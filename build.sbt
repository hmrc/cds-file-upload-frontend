import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "cds-file-upload-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

PlayKeys.devSettings := List("play.server.http.port" -> "6793")

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(commonSettings)
  .settings(scoverageSettings)

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    publish / skip := true,
    Test / testOptions += Tests.Argument("-o", "-h", "it/target/html-report")
  )

lazy val commonSettings = List(
  scalacOptions ++= scalacFlags,
  retrieveManaged := true,
  libraryDependencies ++= Dependencies(),
  routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
  TwirlKeys.templateImports ++= List.empty
)

lazy val scalacFlags = List(
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

// Prevent the "No processor claimed any of these annotations" warning
javacOptions ++= List("-Xlint:-processing")

lazy val scoverageSettings = List(
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
  coverageMinimumStmtTotal := 90,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

addCommandAlias("ucomp", "Test/compile")
addCommandAlias("icomp", "it/Test/compile")
addCommandAlias("precommit", ";clean;scalafmt;Test/scalafmt;it/Test/scalafmt;coverage;test;it/test;scalafmtCheckAll;coverageReport")
