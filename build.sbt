name := "ScalaSearchEngine"
resolvers += "bintray-banno-oss-releases" at "http://dl.bintray.com/banno/oss"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.2"

lazy val Http4sVersion = "0.15.11a"
lazy val doobieVersion = "0.4.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.3" % "test",
  "org.asynchttpclient" % "async-http-client" % "2.0.33",
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  //"org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "org.specs2" %% "specs2-core" % "3.9.1" % "test"
)
