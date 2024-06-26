import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  lazy val codacySeed = "com.codacy" %% "codacy-engine-scala-seed" % "6.1.3"
  lazy val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.34.16"
  lazy val betterFiles = "com.github.pathikrit" %% "better-files" % "3.9.2"

  lazy val cats = "org.typelevel" %% "cats-core" % "2.1.0"
}
