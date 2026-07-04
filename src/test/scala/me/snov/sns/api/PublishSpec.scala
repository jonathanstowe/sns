package me.snov.sns.api

import java.util.concurrent.TimeUnit

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.http.scaladsl.model.{FormData, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.testkit.{TestActor, TestProbe}
import org.apache.pekko.util.Timeout
import me.snov.sns.actor.PublishActor.CmdPublish
import me.snov.sns.model.{Message, MessageAttribute}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PublishSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val timeout: Timeout = new Timeout(3, TimeUnit.SECONDS)

  val probe = TestProbe()
  val route = PublishApi.route(probe.ref)

  "Publish requires topic ARN" in {
    val params = Map("Action" -> "Publish")
    Post("/", FormData(params)) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "Sends publish command" in {
    val params = Map(
      "Action" -> "Publish",
      "TopicArn" -> "foo",
      "Message" -> "bar"
    )

    val routeF: Future[Unit] = Future {
      Post("/", FormData(params)) ~> route ~> check {
      }
    }

    val msg = probe.expectMsgType[CmdPublish](3.seconds)
    probe.reply(Message(Map("default" -> "foo")))

    Await.result(routeF, 5.seconds)
    // verify the publish command received matches expected
    msg shouldBe CmdPublish("foo", Map("default" -> "bar"), Map.empty)
  }

  "Sends publish command to TargetArn" in {
    val params = Map(
      "Action" -> "Publish",
      "TargetArn" -> "foo",
      "Message" -> "bar"
    )

    val routeF: Future[Unit] = Future {
      Post("/", FormData(params)) ~> route ~> check {
      }
    }

    val msg = probe.expectMsgType[CmdPublish](3.seconds)
    probe.reply(Message(Map("default" -> "foo")))

    Await.result(routeF, 5.seconds)
    msg shouldBe CmdPublish("foo", Map("default" -> "bar"), Map.empty)
  }

  "Sends publish command with attributes" in {
    val params = Map(
      "Action" -> "Publish",
      "TopicArn" -> "foo",
      "Message" -> "bar",
      "MessageAttributes.entry.1.Value.DataType" -> "String",
      "MessageAttributes.entry.1.Value.StringValue" -> "AttributeValue",
      "MessageAttributes.entry.1.Name" -> "AttributeName"
    )

    val routeF: Future[Unit] = Future {
      Post("/", FormData(params)) ~> route ~> check {
      }
    }

    val msg = probe.expectMsgType[CmdPublish](3.seconds)
    probe.reply(Message(Map("default" -> "foo"), messageAttributes = Map("AttributeName" -> MessageAttribute("StringValue", "AttributeValue"))))

    Await.result(routeF, 5.seconds)
    msg shouldBe CmdPublish("foo", Map("default" -> "bar"),Map("AttributeName" -> MessageAttribute("StringValue", "AttributeValue")))
  }
}
