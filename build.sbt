organization := "com.gleyzer"

name := "simpleproxy"

version := "SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies += "com.twitter" %% "twitter-server" % "1.3.1"

seq(Revolver.settings: _*)
