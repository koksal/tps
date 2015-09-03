import sbt._
import Keys._

object ApplicationBuild extends Build {

  val osName: String = System.getProperty("os.name")

  val isLinux = osName.indexOf("nux") >= 0
  val isMac   = osName.indexOf("Mac") >= 0

  val osPath = if (isLinux) "linux" else if (isMac) "osx" else "NA"

  val commonSettings = Seq(
    Keys.fork in (Compile, run) := true,
    Keys.fork in Test := true,

    baseDirectory in run := file("."),
    baseDirectory in Test := file("."),

    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

    showSuccess := false,
      
    resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
  )

  val coreSettings = commonSettings ++ Seq(
    outputStrategy := Some(StdoutOutput),

    unmanagedBase <<= baseDirectory { base => base / "lib" / osPath },

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
      "com.github.scopt" %% "scopt" % "3.3.0"
    ),

    mainClass in (Compile, run) := Some("tps.Main"),

    connectInput in run := true
  )

  val core = Project("tps-core", file("tps-core")).settings(
    coreSettings: _*
  )
}
