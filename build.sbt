name := "mySBTbuild"

resolvers += "bintray-banno-oss-releases" at "http://dl.bintray.com/banno/oss"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test,
  "org.asynchttpclient" % "async-http-client" % "2.0.33",
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "org.scala-lang.modules" %% "scala-async" % "0.9.6",
  "org.slf4j" % "slf4j-simple" % "1.7.25" % "runtime",
  "org.specs2" %% "specs2-core" % "3.9.1" % "test"
)
