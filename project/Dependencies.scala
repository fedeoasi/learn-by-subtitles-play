import play.sbt.PlayImport._
import sbt._

object Dependencies {
  object Versions {
    val scalacache = "0.10.0"
    val silencer = "1.7.1"
    val json4s = "3.5.5"
  }

  private val commonsIo = "org.apache.commons" % "commons-io" % "1.3.2"
  private val commonsLang = "commons-lang" % "commons-lang" % "2.6"
  private val elasticSearch = "org.elasticsearch" % "elasticsearch" % "2.4.6"
  private val flyway = "org.flywaydb" % "flyway-core" % "3.2.1"
  private val guava = "com.google.guava" % "guava" % "19.0"
  private val h2 = "com.h2database" % "h2" % "1.4.181" // should this be for tests only?
  private val jacksonCoreAsl = "org.codehaus.jackson" % "jackson-core-asl" % "1.9.11"
  private val joda = "joda-time" % "joda-time" % "2.4"
  private val jodaConvert = "org.joda" % "joda-convert" % "1.7"
  private val json4sJackson = "org.json4s" %% "json4s-jackson" % Versions.json4s
  private val json4sExt = "org.json4s" %% "json4s-ext" % Versions.json4s
  private val logback = "ch.qos.logback" % "logback-classic" % "1.0.13" % "runtime"
  private val mySqlConnector = "mysql" % "mysql-connector-java" % "5.1.32"
  private val scalacacheCore = "com.github.cb372" %% "scalacache-core" % Versions.scalacache
  private val scalacacheGuava = "com.github.cb372" %% "scalacache-guava" % Versions.scalacache
  private val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"
  private val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.6"
  private val scalaTestPlay = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % Test
  private val silencerLib = "com.github.ghik" % "silencer-lib" % Versions.silencer % Provided cross CrossVersion.full
  private val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % Versions.silencer cross CrossVersion.full)
  private val slick = "com.typesafe.slick" %% "slick" % "2.1.0"
  private val sqliteJdbc = "org.xerial" % "sqlite-jdbc" % "3.8.7"

  private val webJars = {
    val handlebars = "org.webjars.bower" % "handlebars" % "2.0.0"
    val jQuery = "org.webjars" % "jquery" % "2.1.4"
    val datatables = "org.webjars" % "datatables" % "1.10.9"
    val datatablesTools = "org.webjars" % "datatables-tools" % "2.2.4-1"
    val d3Cloud = "org.webjars" % "d3-cloud" % "1.0.5"
    val colors = "org.webjars" % "colors" % "0.2.0"
    Seq(handlebars, jQuery, datatables, datatablesTools, d3Cloud, colors)
  }

  private val xmlRpc = "org.apache.xmlrpc" % "xmlrpc" % "3.1.3"
  private val xmlRpcClient = "org.apache.xmlrpc" % "xmlrpc-client" % "3.1.3"

  val allLibraryDependencies: Seq[ModuleID] = Seq(
    jdbc,
    ws,
    flyway,
    xmlRpc,
    xmlRpcClient,
    commonsIo,
    commonsLang,
    slick,
    elasticSearch,
    h2,
    sqliteJdbc,
    guava,
    mySqlConnector,
    json4sJackson,
    json4sExt,
    jacksonCoreAsl,
    logback,
    scalacacheCore,
    scalacacheGuava,
    joda,
    jodaConvert,
    scalaCsv,
    scalaArm,
    scalaTestPlay,
    silencerLib,
    silencerPlugin,
    guice
  ) ++ webJars
}
