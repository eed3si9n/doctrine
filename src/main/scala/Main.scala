package doctrine

import sbt._
import cross.CrossVersionUtil
import Configurations.{ Compile, Test, Runtime }
import java.io.File
import Path._

case class Config(args: Seq[String] = Nil, outDir: File = new File("."))

object Main {
  def start(args: Array[String], appConfig: xsbti.AppConfiguration): Unit = {
    val paramParser = new scopt.OptionParser[Config]("doctrine") {
      head("doctrine", doctrine.BuildInfo.version)
      note("doctrine is an app to download Scala docs to the current directory.")
      opt[File]('o', "out") valueName("<dir>") text("output dir [default: .]") action { (x, c) =>
        c.copy(outDir = x)
      }
      help("help") text("display this message")
      version("version") text("display version info")
      arg[String]("\"org\" % \"foo\" % \"1.0\"") unbounded() text("moduleid for the Scala doc") action { (x, c) =>
        c.copy(args = c.args :+ x)
      }
    }
    paramParser.parse(args, Config()) map { c =>
      processArgs(c.args.toVector, c.outDir, appConfig)
    }
  }
  def processArgs(args: Vector[String], parentDir: File, appConfig: xsbti.AppConfiguration): Unit = {
    val m0 = args match {
      case Seq(org, "%", name, "%", version) =>
        ModuleID(org, name, version, Some("compile"))
      case Seq(org, "%%", name, "%", version) =>
        ModuleID(org, name, version, Some("compile")) cross CrossVersion.binary
      case _ =>
        sys.error("Unknown module format: " + args.mkString(" "))
    }
    val m = m0.intransitive().javadoc()
    val fullScalaVersion =
      m match {
        case m if m.name endsWith "_2.10" => "2.10.4"
        case m if m.name endsWith "_2.11" => "2.11.2"
        case m if m.name == "scala-library" => m.revision
        case _ => "2.11.2"
      }

      if (m.name endsWith "_2.10") "2.10.4"
      else "2.11.2"
    resolveModule(m, appConfig, fullScalaVersion) map { jarFile =>
      val out = parentDir / jarFile.base
      if (out.exists) sys.error(s"$out exists already!")
      else IO.unzip(jarFile, out)
      log.info("unzippped documents to " + out.toString)
      val idx = out.getAbsoluteFile / "index.html"
      if (idx.exists) {
        Browser.open(idx.toURI.toString)
      }
    }
  }
  def resolveModule(m: ModuleID, appConfig: xsbti.AppConfiguration, fullScalaVersion: String): Option[File] = {
    val ivySbt = new IvySbt(mkIvyConfiguration(appConfig))
    val fm = fakeModule(Seq(m), Some(fullScalaVersion), ivySbt)
    val uc = new UpdateConfiguration(None, false, UpdateLogging.DownloadOnly)
    val ur = IvyActions.update(fm, uc, log)
    val mrOpt =
      for {
        cr <- ur.configurations.find(_.configuration == Compile.name)
        mr <- cr.modules find { x =>
          x.module.organization == m.organization &&
          x.module.name == (m.crossVersion match {
            case _: CrossVersion.Binary => m.name + "_" + CrossVersionUtil.binaryScalaVersion(fullScalaVersion)
            case _: CrossVersion.Full   => m.name + "_" + fullScalaVersion
            case _ => m.name
          })
        }
      } yield mr
    val fileOpt = mrOpt flatMap {
      _.artifacts.headOption map {_._2}
    }
    fileOpt
  }
  lazy val log = ConsoleLogger() // nullLogger
  def currentBase: File = new File(".")
  def mkIvyConfiguration(appConfig: xsbti.AppConfiguration): IvyConfiguration = {
    val paths = new IvyPaths(currentBase, bootIvyHome(appConfig))
    val rs = appRepositories(appConfig) getOrElse Seq(DefaultMavenRepository)
    val other = Nil
    val moduleConfs = Seq(ModuleConfiguration("*", DefaultMavenRepository))
    val off = false
    val check = Nil
    // val resCacheDir = currentTarget / "resolution-cache"
    new InlineIvyConfiguration(paths, rs, other, moduleConfs, off, None, check, /*Some(resCacheDir)*/ None, log)
  }
  def fakeModule(deps: Seq[ModuleID], scalaFullVersion: Option[String], ivySbt: IvySbt): IvySbt#Module = {
    val ivyScala = scalaFullVersion map { fv =>
      new IvyScala(
        scalaFullVersion = fv,
        scalaBinaryVersion = CrossVersionUtil.binaryScalaVersion(fv),
        configurations = Nil,
        checkExplicit = true,
        filterImplicit = false,
        overrideScalaVersion = false)
    }
    val moduleSetting: ModuleSettings = InlineConfiguration(
      module = ModuleID("com.example.temp", "fake", "0.1.0-SNAPSHOT", Some("compile")),
      moduleInfo = ModuleInfo(""),
      dependencies = deps,
      configurations = Seq(Compile, Test, Runtime),
      ivyScala = ivyScala)
    new ivySbt.Module(moduleSetting)
  }

  def nullLogger: AbstractLogger = new AbstractLogger {
    def getLevel: Level.Value = Level.Error
    def setLevel(newLevel: Level.Value) {}
    def getTrace = 0
    def setTrace(flag: Int) {}
    def successEnabled = false
    def setSuccessEnabled(flag: Boolean) {}
    def control(event: ControlEvent.Value, message: => String) {}
    def logAll(events: Seq[LogEvent]) {}
    def trace(t: => Throwable) {}
    def success(message: => String) {}
    def log(level: Level.Value, message: => String) {}
  }

  def bootIvyHome(app: xsbti.AppConfiguration): Option[File] =
    try { Option(app.provider.scalaProvider.launcher.ivyHome) }
    catch { case _: NoSuchMethodError => None }
  def appRepositories(app: xsbti.AppConfiguration): Option[Seq[Resolver]] =
    try { Some(app.provider.scalaProvider.launcher.appRepositories.toSeq map bootRepository) }
    catch { case _: NoSuchMethodError => None }

  private[this] def mavenCompatible(ivyRepo: xsbti.IvyRepository): Boolean =
    try { ivyRepo.mavenCompatible }
    catch { case _: NoSuchMethodError => false }
  private[this] def bootRepository(repo: xsbti.Repository): Resolver =
    {
      import xsbti.Predefined
      repo match {
        case m: xsbti.MavenRepository => MavenRepository(m.id, m.url.toString)
        case i: xsbti.IvyRepository => Resolver.url(i.id, i.url)(Patterns(i.ivyPattern :: Nil, i.artifactPattern :: Nil, mavenCompatible(i)))
        case p: xsbti.PredefinedRepository => p.id match {
          case Predefined.Local                => Resolver.defaultLocal
          case Predefined.MavenLocal           => Resolver.mavenLocal
          case Predefined.MavenCentral         => DefaultMavenRepository
          case Predefined.SonatypeOSSReleases  => Resolver.sonatypeRepo("releases")
          case Predefined.SonatypeOSSSnapshots => Resolver.sonatypeRepo("snapshots")
          case unknown                         => sys.error("Unknown predefined resolver '" + unknown + "'.  This resolver may only be supported in newer sbt versions.")
        }
      }
    }
}

// https://github.com/unfiltered/unfiltered/blob/master/util/src/main/scala/utils.scala
object Browser {
  /** Tries to open a browser window, returns Some(exception) on failure */
  def open(loc: String) =
    try {
      import java.net.URI
      val dsk = Class.forName("java.awt.Desktop")
      dsk.getMethod("browse", classOf[URI]).invoke(
        dsk.getMethod("getDesktop").invoke(null), new URI(loc)
      )
      None
    } catch { 
      case _: Throwable => None
    }
}

class SbtApp extends xsbti.AppMain {
  def run(appConfig: xsbti.AppConfiguration) = {
    try {
      Main.start(appConfig.arguments, appConfig)
      Exit(0)
    }
    catch {
      case e: Exception =>
        Console.err.println(e.getMessage)
        Exit(1)
    }
  }

  case class Exit(val code: Int) extends xsbti.Exit
}

