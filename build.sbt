name := "subbot"

organization := "subbot"

version := "0.0.1b"

scalaVersion := "2.12.2"

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.18",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.18",
  "com.typesafe.akka" %% "akka-http" % "10.0.7",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "org.reactivemongo" %% "reactivemongo" % "0.12.3",
  "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)