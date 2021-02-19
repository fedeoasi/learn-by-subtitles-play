name := """learn-by-subtitles"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.13"

import Dependencies._

libraryDependencies ++= allLibraryDependencies

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scalacOptions ++= Seq("-Xfatal-warnings")