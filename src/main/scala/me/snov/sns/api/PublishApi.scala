package me.snov.sns.api

import me.snov.sns.actor.PublishActor.CmdPublish
import me.snov.sns.model.{Message, MessageAttribute, TopicNotFoundException}
import me.snov.sns.response.PublishResponse
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext

case class InvalidTopicArnException(msg: String) extends Exception(msg)

object PublishApi {
  // consistent ARN pattern: alphanumeric, underscore, colon, hyphen
  private val arnPattern = """([A-Za-z0-9_:\-]{1,512})""".r

  def route(actorRef: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Route = {
    pathSingleSlash {
      formField(Symbol("Action")) {
        case "Publish" =>
          formFieldSeq { fields =>
            val messageAttributes: Map[String, MessageAttribute] = MessageAttribute.parse(fields)
            formFields(Symbol("TopicArn").?, Symbol("TargetArn").?, Symbol("MessageStructure").?, Symbol("Message").?) { (topicArnMaybe, targetArnMaybe, messageStructure, messageOpt) =>
              try {
                val msg = messageOpt.getOrElse(throw new RuntimeException("Message is required"))
                topicArn(topicArnMaybe, targetArnMaybe) match {
                  case arnPattern(topic) =>
                    val bodies = messageStructure match {
                      case Some("json") => msg.parseJson.asJsObject.convertTo[Map[String, String]]
                      case Some(_) => throw new RuntimeException("Invalid MessageStructure value");
                      case None => Map("default" -> msg)
                    }

                    val futMsg = (actorRef ? CmdPublish(topic, bodies, messageAttributes)).mapTo[Message]

                    ApiUtils.completeFromFuture(futMsg)(m => complete(PublishResponse.publish(m))) {
                      case t: TopicNotFoundException => complete(PublishResponse.topicNotFound(t.getMessage))
                      case t => complete(HttpResponse(500, entity = t.getMessage))
                    }

                  case _ =>
                    complete(HttpResponse(400, entity = "Invalid topic ARN"))
                }
              } catch {
                case e: InvalidTopicArnException => complete(HttpResponse(400, entity = e.getMessage))
                case e: RuntimeException => complete(HttpResponse(400, entity = e.getMessage))
              }
            }
          } ~
          complete(HttpResponse(400, entity = "TopicArn is required"))

        case _ =>
          reject
      }
    }
  }

  private def topicArn(topicArnMaybe: Option[String], targetArnMaybe: Option[String]): String = {
    topicArnMaybe.getOrElse(targetArnMaybe.getOrElse(throw InvalidTopicArnException("Neither TopicArn nor TargetArn provided")))
  }
}
