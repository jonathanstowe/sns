package me.snov.sns.service

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import org.apache.pekko.event.LoggingAdapter
import me.snov.sns.model.{Configuration, Subscription, Topic}
import spray.json._

import scala.annotation.unused

trait DbService {
  def load(): Option[Configuration]

  def save(configuration: Configuration): Unit
}

@unused
class MemoryDbService extends DbService {
  override def load(): Option[Configuration] = {
    Some(Configuration(subscriptions= List[Subscription](), topics= List[Topic]()))
  }

  override def save(configuration: Configuration): Unit = {}
}

class FileDbService(dbFilePath: String)(implicit log: LoggingAdapter) extends DbService {

  @unused
  val subscriptionsName: String = "subscriptions"
  @unused
  val topicsName: String = "topics"

  val path: Path = Paths.get(dbFilePath)

  def load(): Option[Configuration] = {
    if (Files.exists(path)) {
      log.debug("Loading DB")
      try {
        val configuration = read().parseJson.convertTo[Configuration]
        log.info("Loaded DB")
        return Some(configuration)
      } catch {
        case e: DeserializationException => log.error(e, "Unable to parse configuration")
        case e: RuntimeException => log.error(e,"Unable to load configuration")
      }
    }
    None
  }

  def save(configuration: Configuration): Unit = {
    log.debug("Saving DB")
    write(configuration.toJson.prettyPrint)
  }

  private def write(contents: String) = {
    Files.write(path, contents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def read(): String = {
    new String(Files.readAllBytes(path))
  }
}
