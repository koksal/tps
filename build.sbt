val core = (project in file("tps-core")).settings(
  Keys.fork in (Compile, run) := true,
  Keys.fork in Test := true,

  baseDirectory in run := file("."),
  baseDirectory in Test := file("."),

  scalaVersion := "2.12.12",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

  resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public",

  outputStrategy := Some(StdoutOutput),

  unmanagedBase := baseDirectory.value / "lib" ,

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "com.github.scopt" %% "scopt" % "4.0.0"
  ),

  mainClass in (Compile, run) := Some("tps.Main"),

  connectInput in run := true
)
