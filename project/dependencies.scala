import sbt._
import Keys._

object Dependencies {
  val scala210 = "2.10.4"
  val launcherInterface = "org.scala-sbt" % "launcher-interface" % "0.13.0"
  val sbtIvy = "org.scala-sbt" % "ivy" % "0.13.5"
  val scopt = "com.github.scopt" %% "scopt" % "3.2.0"

  def appDependencies(sv: String) = Seq(
    sbtIvy,
    scopt
  )
}
