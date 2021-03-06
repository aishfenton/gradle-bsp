/*
 * This Scala source file was generated by the Gradle 'init' task.
 */
package gradle.bsp

import java.util.concurrent._
import ch.epfl.scala.bsp4j._
import org.eclipse.lsp4j.jsonrpc.Launcher
import com.typesafe.scalalogging.Logger
import java.util.{Collections, Arrays}

import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters._

import java.nio.file.{Path, Paths}
import org.gradle.tooling.GradleConnector
import java.io.{ByteArrayOutputStream, File}
import scala.util.control.NonFatal
import scala.concurrent.java8.FuturesConvertersImpl
import org.gradle.tooling.ProjectConnection
import scala.util.Try

object App {
  def main(args: Array[String]): Unit = {

    val logger = Logger("app")
    logger.info("STARTING APP")

    val localServer = new MyBuildServer()

    val launcher = new Launcher.Builder[BuildClient]()
      .setOutput(System.out)
      .setInput(System.in)
      .setLocalService(localServer)
      .setRemoteInterface(classOf[BuildClient])
      .create()

    localServer.client = launcher.getRemoteProxy()

    // localServer.client.onBuildLogMessage(
    //   new LogMessageParams(MessageType.ERROR, "I DID A LOG MESSAGE")
    // )
    // localServer.client.onBuildShowMessage(
    //   new ShowMessageParams(MessageType.ERROR, "I DID SHOW MESSAGE")
    // )

    launcher.startListening().get() // listen until BSP session is over.

  }
}

class MyBuildServer extends BuildServer with ScalaBuildServer {
  var client: BuildClient = null // will be updated later
  val logger = Logger("BUILD SERVER")

  val languages = Collections.singletonList("scala")

  val serverCapabilities = new BuildServerCapabilities()
  serverCapabilities.setCompileProvider(new CompileProvider(languages))
  serverCapabilities.setCanReload(true)

  val id = new BuildTargetIdentifier("id")
  val capabilities = new BuildTargetCapabilities(true, false, false)

  var workspace: Path = Paths.get(".").toAbsolutePath.normalize()

  def src: Path = workspace.resolve("app/src/main/scala")
  val out: Path = workspace.resolve("out.jar")

  val scalaTarget = new ScalaBuildTarget(
    "org.scala-lang",
    "2.12.15",
    "2.12",
    ScalaPlatform.JVM,
    List(
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-logging_2.12-3.9.4.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-compiler-2.12.15.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-reflect-2.12.15.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-java8-compat_2.12-1.0.2.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-collection-compat_2.12-2.5.0.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-xml_2.12-1.2.0.jar",
      "file:///vol/src/gradle-bsp/app/build/distributions/app/lib/scala-library-2.12.15.jar"
    ).asJava
  )

  val target: BuildTarget = {
    val result = new BuildTarget(
      id,
      Collections.singletonList("tag"),
      Collections.singletonList("scala"),
      Collections.emptyList(),
      capabilities
    )
    result.setDisplayName("id")
    result.setData(scalaTarget)
    result
  }

  logger.info(s"target is: ${target}")

  lazy val gradleConnection: Try[ProjectConnection] = Try {
    GradleConnector.newConnector
      .forProjectDirectory(new File("."))
      .connect()
  }

  // build/initialize
  def buildInitialize(
      params: InitializeBuildParams
  ): CompletableFuture[InitializeBuildResult] = {
    logger.info(s"build/initialize ${params}")

    CompletableFuture.completedFuture(
      new InitializeBuildResult("My Build Tool", "1", "1", serverCapabilities)
    )
  }

  // build/shutdown
  def buildShutdown(): CompletableFuture[Object] = {
    logger.info(s"build/shutdown")
    CompletableFuture.completedFuture(Int.box(0))
  }

  def buildTargetCleanCache(
      params: CleanCacheParams
  ): CompletableFuture[CleanCacheResult] = {
    logger.info(s"build/shutdown")
    CompletableFuture.completedFuture(
      new CleanCacheResult("I CLEANED IT", true)
    )
  }

  def buildTargetCompile(
      params: CompileParams
  ): CompletableFuture[CompileResult] = {
    logger.info("build/compile")
    val errors = doCompile(params.getTargets.asScala.head)
    publishDiagnostics(errors)
    CompletableFuture.completedFuture(new CompileResult(StatusCode.ERROR))
  }

  def doCompile(id: BuildTargetIdentifier): List[PublishDiagnosticsParams] = {
    logger.info("Connecting to Gradle")
    val connection =
      GradleConnector.newConnector.forProjectDirectory(new File(".")).connect()

    val stdOutStream = new ByteArrayOutputStream
    val stdErrStream = new ByteArrayOutputStream

    logger.info("About to run build on gradle")
    try {
      connection.newBuild
        .forTasks("compileScala")
        .setStandardOutput(stdOutStream)
        .setStandardError(stdErrStream)
        .run()
    } catch {
      // expected if compiler fails
      case NonFatal(e) =>
        logger.error("Unexpected error during gradle compile", e)
    } finally {
      connection.close()
    }
    logger.info("Done")

    val errors = stdErrStream.toString("utf-8")

    logger.info(s"Compile Task Output ${stdOutStream.toString("utf-8")}")
    logger.info(s"Compile Task Error $errors")

    MessageParser.parseCompileError(id, errors)
  }

  def buildTargetDependencySources(
      params: DependencySourcesParams
  ): CompletableFuture[DependencySourcesResult] = {
    logger.info(s"buildTargetDependencySources: $params")
    CompletableFuture.completedFuture {
      new DependencySourcesResult(
        List(
          new DependencySourcesItem(
            target.getId,
            List().asJava
          )
        ).asJava
      )
    }
  }

  def buildTargetDependencyModules(
      params: DependencyModulesParams
  ): CompletableFuture[DependencyModulesResult] = ???

  def buildTargetInverseSources(
      params: InverseSourcesParams
  ): CompletableFuture[InverseSourcesResult] = ???

  def buildTargetResources(
      params: ResourcesParams
  ): CompletableFuture[ResourcesResult] = ???

  def buildTargetRun(params: RunParams): CompletableFuture[RunResult] = ???

  def buildTargetSources(
      params: SourcesParams
  ): CompletableFuture[SourcesResult] = {
    logger.info(s"buildTargetSources: ${params}")
    CompletableFuture.completedFuture(
      new SourcesResult(
        List(
          new SourcesItem(
            target.getId,
            List(
              new SourceItem(
                src.toUri.toString,
                SourceItemKind.DIRECTORY,
                false
              )
            ).asJava
          )
        ).asJava
      )
    )
  }

  def buildTargetTest(params: TestParams): CompletableFuture[TestResult] = ???

  // build/exit
  def onBuildExit(): Unit = {
    logger.info(s"build/exit")
    System.exit(0)
  }

  // build/initialized
  def onBuildInitialized(): Unit = {
    logger.info("build/initialized")
  }

  def workspaceBuildTargets()
      : CompletableFuture[WorkspaceBuildTargetsResult] = {
    logger.info("workspaceBuildTargets")
    val targets = fetchTargets.get
    logger.info(s"Found build targets: ${targets.map(_.getDisplayName)}")
    CompletableFuture.completedFuture(new WorkspaceBuildTargetsResult(targets.asJava))
  }

  def fetchTargets: Try[List[BuildTarget]] = for {
    conn <- gradleConnection
    targets <- Try {
      val model =
        conn.getModel(classOf[org.gradle.tooling.model.gradle.GradleBuild])
      model.getProjects.asScala.map { proj =>
        val target = new BuildTarget(
          new BuildTargetIdentifier(proj.getName),
          List.empty.asJava,
          languages,
          List.empty.asJava,
          capabilities
        )
        target.setData(scalaTarget)
        target.setDisplayName(proj.getName)
        target
      }.toList
    }
  } yield targets

  import scala.concurrent.ExecutionContext.Implicits.global
  def workspaceReload(): CompletableFuture[Object] = {
    scala.concurrent.Future
      .apply {
        val event = new BuildTargetEvent(target.getId)
        event.setKind(BuildTargetEventKind.CHANGED)
        client.onBuildTargetDidChange(
          new DidChangeBuildTarget(List(event).asJava)
        )
        null: Object
      }
      .toJava
      .toCompletableFuture
  }

  // buildTarget/scalacOptions
  def buildTargetScalacOptions(
      params: ScalacOptionsParams
  ): CompletableFuture[ScalacOptionsResult] = {
    logger.info(s"buildTarget/scalacOptions ${params}")
    CompletableFuture.completedFuture(
      new ScalacOptionsResult(
        Arrays.asList(
          new ScalacOptionsItem(
            target.getId,
            List().asJava,
            List().asJava,
            out.toAbsolutePath.toUri.toASCIIString
          )
        )
      )
    )
  }

  // buildTarget/scalaTestClasses
  def buildTargetScalaTestClasses(
      params: ScalaTestClassesParams
  ): CompletableFuture[ScalaTestClassesResult] = {
    logger.info("buildTargetScalaTestClasses")
    CompletableFuture.completedFuture(
      new ScalaTestClassesResult(
        List(new ScalaTestClassesItem(target.getId, List().asJava)).asJava
      )
    )
  }

  // buildTarget/scalaMainClasses
  def buildTargetScalaMainClasses(
      params: ScalaMainClassesParams
  ): CompletableFuture[ScalaMainClassesResult] = {
    logger.info("buildTargetScalaMainClasses")
    CompletableFuture.completedFuture(
      new ScalaMainClassesResult(
        List(new ScalaMainClassesItem(target.getId, List().asJava)).asJava
      )
    )
  }

  def publishDiagnostics(errors: List[PublishDiagnosticsParams]): Unit = {
    logger.info(s"publishing diagnostic: $errors")
    errors.foreach(client.onBuildPublishDiagnostics)
  }

}

object MessageParser {

  private val MsgPattern = """\[(Error|Warn)\] ([^:]+):(\d+):(\d+): (.*)""".r

  private def mkDiagnostic(line: Int, col: Int, msg: String): Diagnostic = {
    val diag = new Diagnostic(
      new Range(
        new Position(Int.box(line) - 1, Int.box(col)),
        new Position(Int.box(line) - 1, Int.box(col + 1))
      ),
      msg
    )
    diag.setSeverity(DiagnosticSeverity.ERROR)
    diag
  }

  private def mkPublishDiagnosticsParams(
      targetId: BuildTargetIdentifier,
      file: String,
      line: Int,
      col: Int,
      msg: String
  ): PublishDiagnosticsParams = new PublishDiagnosticsParams(
    new TextDocumentIdentifier("file://" + file),
    targetId,
    List(mkDiagnostic(line, col, msg)).asJava,
    true
  )

  def parseCompileError(
      targetId: BuildTargetIdentifier,
      errorMsg: String
  ): List[PublishDiagnosticsParams] = {
    MsgPattern
      .findAllMatchIn(errorMsg)
      .map { m =>
        val severity = m.group(1)
        val file = m.group(2)
        val line = m.group(3).toInt
        val col = m.group(4).toInt
        val msg = m.group(5)

        mkPublishDiagnosticsParams(targetId, file, line, col, msg)
      }
      .toList
  }

}
