// Mnemocast Engine - Build Configuration
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

lazy val circeVersion      = "0.14.7"
lazy val jedisVersion      = "5.1.0"
lazy val pekkoVersion      = "1.0.2"
lazy val pekkoHttpVersion  = "1.0.1"
lazy val pekkoCirceVersion = "2.8.0"

lazy val root = (project in file("."))
  .aggregate(engineDomain, engineInfra, engineApi)
  .dependsOn(engineApi)
  .settings(
    name := "mnemocast-engine",
    Compile / mainClass := Some("mnemocast.engine.api.HttpServer")
  )

// ----------------------
// engine-domain (models)
// ----------------------
lazy val engineDomain = (project in file("modules/engine-domain"))
  .settings(
    name := "engine-domain",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion
    )
  )

// ----------------------
// engine-infra (Redis, etc.)
// ----------------------
lazy val engineInfra = (project in file("modules/engine-infra"))
  .dependsOn(engineDomain)
  .settings(
    name := "engine-infra",
    libraryDependencies ++= Seq(
      "redis.clients" %  "jedis"         % jedisVersion,
      "io.circe"      %% "circe-core"    % circeVersion,
      "io.circe"      %% "circe-generic" % circeVersion,
      "io.circe"      %% "circe-parser"  % circeVersion
    )
  )

// ----------------------
// engine-api (Pekko HTTP)
// ----------------------
lazy val engineApi = (project in file("modules/engine-api"))
  .dependsOn(engineDomain, engineInfra)
  .settings(
    name := "engine-api",
    Compile / mainClass := Some("mnemocast.engine.api.HttpServer"),
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
      "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,

      "com.github.pjfanning" %% "pekko-http-circe" % pekkoCirceVersion,

      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion
    )
  )
