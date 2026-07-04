package me.snov.sns.api

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

object HealthCheckApi {
  val route: Route =
    path("health") {
      get {
        complete("OK")
      }
    }
}
