
scalaVersion := "2.12.7"


name := "hemi-download-as-excel"
organization := "com.selmank"
version := "1.0"

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies += "org.typelevel" %% "cats-core" % "1.4.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.scalaxb" %% "scalaxb" % "1.7.0"
libraryDependencies += "org.scala-lang.modules" % "scala-swing_2.12" % "2.0.3"
libraryDependencies ++= Seq(
  "com.norbitltd" %% "spoiwo" % "1.4.1"
)


libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"
val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-optics"
).map(_ % circeVersion)

libraryDependencies += "org.gnieh" % "diffson-circe_2.12" % "3.1.0"
