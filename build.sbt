name := "Reson"

version := "1.0"

scalaVersion := "2.13.5"

resolvers += "bintray-djspiewak-maven" at "https://dl.bintray.com/djspiewak/maven"

libraryDependencies ++= Seq(
  // finagle pulls invalid jackson version
  "com.twitter" %% "finagle-http" % "21.2.0" excludeAll
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
  "com.twitter" %% "finagle-mysql" % "21.2.0" excludeAll
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1",
  "org.json4s" %% "json4s-jackson" % "3.7.0-M8",
  "org.specs2" %% "specs2-core" % "4.10.6" % "test",
  "junit" % "junit" % "4.13.2" % "test"
)

enablePlugins(PackPlugin)

packMain := Map(
  "server" -> "reson.Server"
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")
