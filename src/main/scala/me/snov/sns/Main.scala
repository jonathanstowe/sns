package me.snov.sns


import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.{Logging, LoggingAdapter}
import org.apache.pekko.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App  {
  implicit val system: ActorSystem = ActorSystem("sns")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val logger: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = new Timeout(1.second)

  Server.start()

  try {
    Await.result(system.whenTerminated, Duration.Inf)
  }
  catch {
    case _: InterruptedException => Thread.currentThread().interrupt()
  }
}
