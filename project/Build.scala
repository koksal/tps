import sbt._
import Keys._

object ApplicationBuild extends Build {

  val commonSettings = Seq(
    Keys.fork in (Compile, run) := true,
    Keys.fork in Test := true,

    baseDirectory in run := file("."),
    baseDirectory in Test := file("."),

    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

    showSuccess := false,
      
    resolvers ++= Seq(
      "sonatype-public" at 
        "https://oss.sonatype.org/content/groups/public",
      "Sonatype Snapshots" at 
        "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype Releases" at 
        "https://oss.sonatype.org/content/repositories/releases/"
    )
  )

  val coreSettings = commonSettings ++ Seq(
    outputStrategy := Some(StdoutOutput),

    unmanagedBase <<= baseDirectory { base => base / "lib" },

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
      "com.github.scopt" %% "scopt" % "3.3.0",
      "org.scalanlp" %% "breeze" % "0.12"
    ),

    mainClass in (Compile, run) := Some("tps.Main"),

    connectInput in run := true
  )

  val core = Project("tps-core", file("tps-core")).settings(
    coreSettings: _*
  )
}
