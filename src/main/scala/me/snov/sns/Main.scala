package me.snov.sns

import com.typesafe.config.ConfigFactory
import me.snov.sns.actor._
import me.snov.sns.api._
import me.snov.sns.service.FileDbService
import me.snov.sns.util.ToStrict
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.{Logging, LoggingAdapter}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server._
import org.apache.pekko.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Properties, Success}

object Main extends App with ToStrict {
  implicit val system: ActorSystem = ActorSystem("sns")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val logger: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = new Timeout(1.second)

  val config = ConfigFactory.load()
  val dbService = new FileDbService(Properties.envOrElse("DB_PATH", config.getString("db.path")))

  val dbActor = system.actorOf(DbActor.props(dbService), name = "DbActor")
  val homeActor = system.actorOf(HomeActor.props, name = "HomeActor")
  val subscribeActor = system.actorOf(SubscribeActor.props(dbActor), name = "SubscribeActor")
  val publishActor = system.actorOf(PublishActor.props(subscribeActor), name = "PublishActor")

  val routes: Route =
    toStrict {
      TopicApi.route(subscribeActor) ~
      SubscribeApi.route(subscribeActor) ~
      PublishApi.route(publishActor) ~
      HealthCheckApi.route ~
      HomeApi.route(homeActor)
    }

  logger.info("SNS v{} is starting", getClass.getPackage.getImplementationVersion)

  val bindingFuture = Http().newServerAt(
    Properties.envOrElse("HTTP_INTERFACE", config.getString("http.interface")),
    Properties.envOrElse("HTTP_PORT", config.getString("http.port")).toInt
  ).bindFlow(logRequestResult("pekko-http-sns")(routes))

  bindingFuture.onComplete {
    case Success(binding) =>
      logger.info(s"Server started at http://${binding.localAddress.toString}")

    case Failure(ex) =>
      logger.error(s"Server failed to start: ${ex.getMessage}")
      ex.printStackTrace()
      system.terminate()
  }
  Await.result(system.whenTerminated, Duration.Inf)
}
