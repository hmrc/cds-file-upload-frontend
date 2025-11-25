import sbt.*

object Dependencies {

  val bootstrapPlayVersion = "9.19.0"

  val compile: Seq[ModuleID] = List(
    "uk.gov.hmrc"               %% "bootstrap-frontend-play-30"     % bootstrapPlayVersion,
    "uk.gov.hmrc"               %% "play-frontend-hmrc-play-30"     % "12.20.0",
    "uk.gov.hmrc"               %% "play-partials-play-30"          % "10.2.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-30"             % "2.10.0",
    "com.github.pureconfig"     %% "pureconfig"                     % "0.17.7"
  ).map(_.withSources)

  val test: Seq[ModuleID] = List(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapPlayVersion % "test",
    "org.mockito"            %% "mockito-scala"           % "1.17.37"            % "test",
    "org.scalatestplus"      %% "scalacheck-1-18"         % "3.2.19.0"           % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.64.8"             % "test",
    "org.jsoup"              %  "jsoup"                   % "1.18.1"             % "test"
  )

  private val missingSources = List("accessible-autocomplete", "flexmark-all")

  def apply(): Seq[ModuleID] =
    (compile ++ test).map(moduleId => if (missingSources.contains(moduleId.name)) moduleId else moduleId.withSources)
}
