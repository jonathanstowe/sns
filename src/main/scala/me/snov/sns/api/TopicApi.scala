package me.snov.sns.api

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Status.Success
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import me.snov.sns.actor.SubscribeActor.{CmdListTopics, CmdDeleteTopic, CmdCreateTopic}
import me.snov.sns.model.Topic
import me.snov.sns.response.TopicResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object TopicApi {
  // name: alphanumeric, underscore, plus, hyphen; arn: alphanumeric, underscore, colon, hyphen
  private val namePattern = """([A-Za-z0-9_+\-]{1,256})""".r
  private val arnPattern = """([A-Za-z0-9_:\-]{1,512})""".r

  def route(actor: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Route =
    pathSingleSlash {
      formField(Symbol("Action")) {
        case "CreateTopic" =>
          formField(Symbol("Name").?) {
            case Some(namePattern(name)) =>
              val f: Future[Topic] = (actor ? CmdCreateTopic(name)).mapTo[Topic]
              onSuccess(f) { topic => complete(TopicResponse.create(topic)) }

            case Some(_) =>
              complete(HttpResponse(400, entity = "InvalidParameter: invalid topic name"))

            case None =>
              complete(HttpResponse(400, entity = "Topic name is missing"))
          }

        case "DeleteTopic" =>
          formField(Symbol("TopicArn").?) {
            case Some(arnPattern(arn)) =>
              val f = (actor ? CmdDeleteTopic(arn)).map {
                case Success => TopicResponse.delete
                case _ => HttpResponse(404, entity = "NotFound")
              }
              onSuccess(f) { resp => complete(resp) }

            case Some(_) =>
              complete(HttpResponse(400, entity = "Invalid topic ARN"))

            case None =>
              complete(HttpResponse(400, entity = "TopicArn is missing"))
          }

        case "ListTopics" =>
          val f = (actor ? CmdListTopics()).mapTo[Iterable[Topic]].map(TopicResponse.list)
          onSuccess(f) { resp => complete(resp) }

        case _ =>
          reject
          // complete(HttpResponse(400, entity = "Action is required (CreateTopic/DeleteTopic/ListTopics)"))
      }
    }
}
