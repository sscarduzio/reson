name := "Reson"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.31.0",
  "com.twitter" %% "finagle-mysql" % "6.31.0",
  "com.propensive" %% "rapture-json-argonaut" % "2.0.0-M1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.11" % "test"
)

packSettings

packMain := Map(
  "server" -> "reson.Server"
)
