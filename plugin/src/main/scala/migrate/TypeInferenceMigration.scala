package migrate

import migrate.interfaces.CompilationException
import ScalaMigratePlugin.{ migrateAPI, inputsStore, scala3Version, Keys }

import sbt.Keys._
import sbt._

import scala.io.AnsiColor._
import scala.util.{ Try, Success, Failure }
import scala.collection.JavaConverters._

import java.nio.file.Files

object TypeInferenceMigration {
  val internalImpl = Def.taskDyn {
    val projectRef = thisProjectRef.value
    val configs    = Keys.migrationConfigs.value
    val sv         = scalaVersion.value

    val projectId = projectRef.project

    if (sv != scala3Version)
      sys.error(s"expecting scalaVersion to be $scala3Version")

    Def.task {
      val logger          = streams.value.log
      val allScala3Inputs = configs.map(_ / Keys.scala3Inputs).join.value

      logger.info(welcomeMessage(projectId, configs.map(_.id)))

      for {
        (scala3Inputs, config) <- allScala3Inputs.zip(configs)
        scope                   = (projectRef / config / Keys.scala2Inputs).scope
        scala2Inputs            = inputsStore.getOrElse(scope, sys.error("no input found"))
        if (scala2Inputs.unmanagedSources.nonEmpty)
      } {
        if (!Files.exists(scala3Inputs.classDirectory))
          Files.createDirectory(scala3Inputs.classDirectory)

        Try {
          migrateAPI.migrate(
            scala2Inputs.unmanagedSources.asJava,
            scala2Inputs.managedSources.asJava,
            scala3Inputs.semanticdbTarget,
            scala2Inputs.classpath.asJava,
            scala2Inputs.scalacOptions.asJava,
            scala3Inputs.classpath.asJava,
            scala3Inputs.scalacOptions.asJava,
            scala3Inputs.classDirectory
          )
        } match {
          case Success(_) =>
            logger.info(successMessage(projectId, config.id))
          case Failure(_: CompilationException) =>
            val message = errorMessage(projectId, config.id, None)
            throw new MessageOnlyException(message)
          case Failure(exception) =>
            val message = errorMessage(projectId, config.id, Some(exception))
            throw new MessageOnlyException(message)
        }
      }

      logger.info(finalMessage(projectId))
    }
  }

  private def welcomeMessage(projectId: String, configs: Seq[String]): String =
    s"""|
        |${BOLD}We are going to migrate $projectId / ${configs.mkString("[", ", ", "]")} to $scala3Version${RESET}
        |
        |""".stripMargin

  private def successMessage(projectId: String, config: String): String =
    s"""|
        |$projectId / $config has been successfully migrated to Scala $scala3Version
        |
        |""".stripMargin

  private def errorMessage(projectId: String, config: String, exceptionOpt: Option[Throwable]) = {
    val exceptionError = exceptionOpt.map { error =>
      s"""|because of ${error.getMessage}
          |${error.getStackTrace.mkString("\n")}""".stripMargin
    }
      .getOrElse("")

    s"""|
        |Migration of $projectId / $config has failed
        |$exceptionError
        |
        |""".stripMargin
  }

  private def finalMessage(projectId: String) =
    s"""|
        |You can now commit the change!
        |Then you can permanently change the scalaVersion of $projectId:
        |
        |crossScalaVersions += $scala3Version  // or
        |scalaVersion := $scala3Version
        |
        |""".stripMargin
}
