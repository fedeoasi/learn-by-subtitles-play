name := """learn-by-subtitles"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

import Dependencies._

libraryDependencies ++= allLibraryDependencies

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalacOptions ++= Seq("-Xfatal-warnings")