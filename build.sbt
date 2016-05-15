name := """learn-by-subtitles"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

val joda = "joda-time" % "joda-time" % "2.4"
val jodaConvert = "org.joda" % "joda-convert" % "1.7"

val webJars = {
  val handlebars = "org.webjars.bower" % "handlebars" % "2.0.0"
  val jQuery = "org.webjars" % "jquery" % "2.1.4"
  val datatables = "org.webjars" % "datatables" % "1.10.9"
  val datatablesTools = "org.webjars" % "datatables-tools" % "2.2.4-1"
  val d3Cloud = "org.webjars" % "d3-cloud" % "1.0.5"
  val colors = "org.webjars" % "colors" % "0.2.0"
  Seq(handlebars, jQuery, datatables, datatablesTools, d3Cloud, colors)
}

lazy val flyway = "org.flywaydb" % "flyway-core" % "3.2.1"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  flyway,
  "org.apache.xmlrpc" % "xmlrpc" % "3.1.3",
  "org.apache.xmlrpc" % "xmlrpc-client" % "3.1.3",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "commons-lang" % "commons-lang" % "2.6",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.elasticsearch" % "elasticsearch" % "2.3.1",
  "com.h2database" % "h2" % "1.4.181",
  "org.xerial" % "sqlite-jdbc" % "3.8.7",
  "com.google.guava" % "guava" % "18.0",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.codehaus.jackson" % "jackson-core-asl" % "1.9.11",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "ch.qos.logback" % "logback-classic" % "1.0.13" % "runtime",
  joda,
  jodaConvert,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
) ++ webJars

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
