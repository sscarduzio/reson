name := "Reson"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "bintray-djspiewak-maven" at "https://dl.bintray.com/djspiewak/maven"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.33.0",
  "com.twitter" %% "finagle-mysql" % "6.33.0",
  "com.propensive" %% "rapture-json-jackson" % "2.0.0-M3",
  "org.specs2" %% "specs2-core" % "3.6.6-scalaz-7.2.0" % "test",
  "junit" % "junit" % "4.11" % "test"
)

packSettings

packMain := Map(
  "server" -> "reson.Server"
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")