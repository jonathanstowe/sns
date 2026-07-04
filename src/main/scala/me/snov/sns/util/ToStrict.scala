package me.snov.sns.util

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.server.Directives._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ToStrict {
  implicit val system: ActorSystem
  implicit val executor: ExecutionContext
  
  val toStrict = mapInnerRoute { innerRoute =>
    val timeout = 1.second
    extractRequest { req =>
      onSuccess(req.toStrict(timeout)) { strictReq =>
        mapRequest(_ => strictReq) {
          innerRoute
        }
      }
    }
  }
}
