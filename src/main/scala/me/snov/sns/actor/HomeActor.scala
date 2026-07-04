package me.snov.sns.actor

import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpResponse}

object HomeActor {
  def props: Props = Props[HomeActor]()

  case class CmdHello()
}

class HomeActor extends Actor {
  import me.snov.sns.actor.HomeActor._

  private def hello: HttpResponse = HttpResponse(entity = HttpEntity("Hello, Pekko"))

  override def receive: Receive = {
    case CmdHello => sender() ! hello
    case _ => sender() ! HttpResponse(500, entity = "Invalid message")
  }
}
