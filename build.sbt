// See LICENSE for license details.

name := "chisel-mips"

organization := "top.emptystack"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.12.10"

val chiselVersion = "3.4.+"
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % chiselVersion,
  "edu.berkeley.cs" %% "chiseltest" % "0.3.0" % "test"
)

scalacOptions ++= Seq(
  // Required options for Chisel code
  "-Xsource:2.11",
  // Recommended options
  "-language:reflectiveCalls",
  "-deprecation",
  "-feature",
  "-Xcheckinit"
)

mainClass in (Compile, run) := Some("cpu.Top")