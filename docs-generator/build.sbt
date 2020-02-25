import Dependencies._

lazy val root = project.in(file("."))
  .settings(
    inThisBuild(
      List(organization := "com.codacy", scalaVersion := "2.13.1", version := "0.1.0-SNAPSHOT")
    ),
    name := "hadolint-docs",
    libraryDependencies ++= Seq(codacySeed, flexmark, cats, scalaTest % Test)
  )
