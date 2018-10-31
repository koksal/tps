import sbt._
import Keys._

object ApplicationBuild extends Build {

  val commonSettings = Seq(
    Keys.fork in (Compile, run) := true,
    Keys.fork in Test := true,

    baseDirectory in run := file("."),
    baseDirectory in Test := file("."),

    scalaVersion := "2.12.4",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

    showSuccess := false,
      
    resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
  )

  val coreSettings = commonSettings ++ Seq(
    outputStrategy := Some(StdoutOutput),

    unmanagedBase <<= baseDirectory { base => base / "lib" },

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.12" % "3.0.1" % "test",
      "com.github.scopt" %% "scopt" % "3.7.0"
    ),

    mainClass in (Compile, run) := Some("tps.Main"),

    connectInput in run := true
  )

  val core = Project("tps-core", file("tps-core")).settings(
    coreSettings: _*
  )
}
