import sbt._
import Keys._

lazy val core = (project in file("."))
  .driverLibrary("pds-ui-common")
  .settings(lintingSettings)
  .settings(scalacOptions -= "-Xfatal-warnings") // this is needed to ignore unused implicits that are actually used in scala 2.11
  .settings(sources in (Compile, doc) := Seq.empty, publishArtifact in (Compile, packageDoc) := false)
  .settings(libraryDependencies ++= Seq(
    "com.github.pureconfig"             %% "pureconfig"             % "0.7.2",
    "com.lihaoyi"                       %% "fastparse"              % "1.0.0",
    "com.typesafe.akka"                 %% "akka-http"              % "10.0.10",
    "com.typesafe.scala-logging"        %% "scala-logging"          % "3.5.0",
    "io.github.cloudify"                %% "spdf"                   % "1.4.0",
    "xyz.driver"                        %% "core"                   % "1.6.12",
    "xyz.driver"                        %% "domain-model"           % "0.21.16",
    "ch.qos.logback"                    % "logback-classic"         % "1.1.7",
    "com.fasterxml.jackson.datatype"    % "jackson-datatype-jsr310" % "2.8.4",
    "com.github.spullara.mustache.java" % "scala-extensions-2.11"   % "0.9.4",
    "com.google.cloud"                  % "google-cloud-storage"    % "1.2.1",
    "com.sendgrid"                      % "sendgrid-java"           % "3.1.0" exclude ("org.mockito", "mockito-core"),
    "com.typesafe"                      % "config"                  % "1.3.0",
    "org.asynchttpclient"               % "async-http-client"       % "2.0.24",
    "org.slf4j"                         % "slf4j-api"               % "1.7.21",
    "org.scalacheck"                    %% "scalacheck"              % "1.13.4" % "test",
    "org.scalatest"                     %% "scalatest"              % "3.0.1"  % "test"
  ))
