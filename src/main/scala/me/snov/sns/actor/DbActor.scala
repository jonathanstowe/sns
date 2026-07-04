package me.snov.sns.actor

import org.apache.pekko.actor.{Actor, ActorLogging, ActorRef, Props}
import me.snov.sns.model.Configuration
import me.snov.sns.service.DbService

object DbActor {
  def props(dbService: DbService) = Props(classOf[DbActor], dbService)

  case class CmdGetConfiguration()
}

class DbActor(dbService: DbService) extends Actor with ActorLogging {
  import me.snov.sns.actor.DbActor._

  val configuration: Option[Configuration] = dbService.load()

  private def replyWithConfiguration(actorRef: ActorRef): Unit = {
    if (configuration.isDefined) {
      actorRef ! configuration.get
    }
  }

  override def receive: Receive = {
    case CmdGetConfiguration => replyWithConfiguration(sender())
    case c: Configuration => dbService.save(c)
  }
}
