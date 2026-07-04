package me.snov.sns.api

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import me.snov.sns.actor.HomeActor.CmdHello

import scala.concurrent.ExecutionContext

object HomeApi {
  def route(actorRef: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Route = {
    pathSingleSlash {
      val fut = (actorRef ? CmdHello).mapTo[HttpResponse]
      ApiUtils.completeFromFutureOr500(fut) { resp =>
        complete(resp)
      }
    }
  }
}
