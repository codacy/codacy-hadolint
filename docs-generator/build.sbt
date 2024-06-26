import Dependencies._

lazy val root = project.in(file("."))
  .settings(
    inThisBuild(
      List(organization := "com.codacy", scalaVersion := "2.13.14", version := "0.1.0-SNAPSHOT")
    ),
    name := "hadolint-docs",
    libraryDependencies ++= Seq(codacySeed, betterFiles, flexmark, cats, scalaTest % Test)
  )
