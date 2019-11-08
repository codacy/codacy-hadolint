import Dependencies._

resolvers := Seq(
  "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/releases"),
  "Typesafe Repo".at("http://repo.typesafe.com/typesafe/releases/")
) ++ resolvers.value

lazy val root = project.in(file("."))
  .settings(
    inThisBuild(
      List(organization := "com.example", scalaVersion := "2.12.6", version := "0.1.0-SNAPSHOT")
    ),
    name := "hadolint-docs",
    libraryDependencies ++= Seq(codacySeed, flexmark, cats, scalaTest % Test)
  )
