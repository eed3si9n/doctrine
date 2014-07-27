import Common._
import Dependencies._

lazy val root = (project in file(".")).
  settings(sonatypeSettings: _*).
  settings(buildInfoSettings: _*).
  settings(
    organization := "com.eed3si9n",
    name := "doctrine",
    version := "0.1.0",
    scalacOptions := Seq("-deprecation", "-unchecked"),
    scalaVersion := scala210,
    homepage := Some(url("http://eed3si9n.com")),
    licenses := Seq("MIT License" -> url("http://opensource.org/licenses/MIT")),
    description := "doctorine is for downloading scala docs.",
    buildInfoPackage := "doctrine",
    sourceGenerators in Compile <+= buildInfo,
    libraryDependencies ++= appDependencies(scalaVersion.value),
    resolvers += Resolver.typesafeIvyRepo("releases")
  )
