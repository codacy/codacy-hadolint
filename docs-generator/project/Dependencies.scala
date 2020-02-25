import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  lazy val codacySeed = "com.codacy" %% "codacy-engine-scala-seed" % "4.0.0"
  lazy val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.34.16"

  lazy val cats = "org.typelevel" %% "cats-core" % "2.1.0"
}
