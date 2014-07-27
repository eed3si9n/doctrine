import sbt._
import Keys._

object Common {
  val sonatypeSettings: Seq[Def.Setting[_]] = Seq(
    pomExtra := (<scm>
        <url>git@github.com:eed3si9n/doctrine.git</url>
        <connection>scm:git:git@github.com:eed3si9n/doctrine.git</connection>
      </scm>
      <developers>
        <developer>
          <id>eed3si9n</id>
          <name>Eugene Yokota</name>
          <url>http://eed3si9n.com</url>
        </developer>
      </developers>),
    publishArtifact in Test := false,
    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/repositories/public"),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots") 
      else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    pomIncludeRepository := { x => false }
  )
}
