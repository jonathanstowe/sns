package me.snov.sns.api

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Status.{Success, Failure}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import me.snov.sns.actor.SubscribeActor.{CmdListSubscriptions, CmdListSubscriptionsByTopic, CmdSubscribe, CmdUnsubscribe,CmdSetSubscriptionAttributes,CmdGetSubscriptionAttributes}
import me.snov.sns.model.Subscription
import me.snov.sns.response.SubscribeResponse

import scala.concurrent.ExecutionContext

object SubscribeApi {
  // consistent ARN pattern across APIs
  private val arnPattern = """([A-Za-z0-9_:\-]{1,512})""".r

  def route(actorRef: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Route = {
    pathSingleSlash {
      formField(Symbol("Action")) {
        case "Subscribe" =>
          formFields(Symbol("Endpoint"), Symbol("Protocol"), Symbol("TopicArn")) { (endpoint, protocol, topicArn) =>
            val fut = (actorRef ? CmdSubscribe(topicArn, protocol, endpoint)).mapTo[Subscription]
            ApiUtils.completeFromFutureOr500(fut) { sub => complete(SubscribeResponse.subscribe(sub)) }
          } ~
            complete(HttpResponse(400, entity = "Endpoint, Protocol, TopicArn are required"))

        case "ListSubscriptionsByTopic" =>
          formField(Symbol("TopicArn")) {
            case arnPattern(topicArn) =>
              val fut = (actorRef ? CmdListSubscriptionsByTopic(topicArn)).mapTo[Iterable[Subscription]]
              ApiUtils.completeFromFutureOr500(fut) { subs => complete(SubscribeResponse.listByTopic(subs)) }
            case _ => complete(HttpResponse(400, entity = "Invalid topic ARN"))
          } ~
            complete(HttpResponse(400, entity = "TopicArn is missing"))

        case "ListSubscriptions" =>
          val fut = (actorRef ? CmdListSubscriptions()).mapTo[Iterable[Subscription]]
          ApiUtils.completeFromFutureOr500(fut) { subs => complete(SubscribeResponse.list(subs)) }

        case "Unsubscribe" =>
          formField(Symbol("SubscriptionArn")) { arn =>
            val fut = (actorRef ? CmdUnsubscribe(arn)).map {
              case Success => SubscribeResponse.unsubscribe
              case _ => HttpResponse(404, entity = "NotFound")
            }
            ApiUtils.completeFromFutureOr500(fut) { resp => complete(resp) }
          } ~
          complete(HttpResponse(400, entity = "SubscriptionArn is missing"))

        case "SetSubscriptionAttributes" =>
          formField(Symbol("SubscriptionArn"), Symbol("AttributeName"), Symbol("AttributeValue")) { (arn, name, value) =>
            val fut = (actorRef ? CmdSetSubscriptionAttributes(arn, name, value)).map {
              case Success => SubscribeResponse.setSubscriptionAttributes()
              case Failure(_) => HttpResponse(404, entity = "NotFound")
            }
            ApiUtils.completeFromFutureOr500(fut) { resp => complete(resp) }
          } ~
          complete(HttpResponse(400, entity = "SubscriptionArn is missing"))

        case "GetSubscriptionAttributes" =>
          formField(Symbol("SubscriptionArn")) { arn =>
            val fut = (actorRef ? CmdGetSubscriptionAttributes(arn)).mapTo[Option[Map[String,String]]]
            ApiUtils.completeFromFutureOr500(fut) { attributes =>
              val resp = attributes
                .map(SubscribeResponse.getSubscriptionAttributes)
                .getOrElse {
                  HttpResponse(404, entity = "Not Found")
                }
              complete(resp)
            }
          } ~
          complete(HttpResponse(400, entity = "SubscriptionArn is missing"))

        case _ =>
          reject
      }
    }
  }
}
