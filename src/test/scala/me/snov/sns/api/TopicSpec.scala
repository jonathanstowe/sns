package me.snov.sns.api

import java.util.concurrent.TimeUnit

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.http.scaladsl.model.{FormData, HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.testkit.{TestActor, TestProbe}
import org.apache.pekko.util.Timeout
import me.snov.sns.actor.SubscribeActor.{CmdDeleteTopic, CmdCreateTopic}
import me.snov.sns.model.Topic
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TopicSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val timeout: Timeout = new Timeout(3, TimeUnit.SECONDS)

  val probe = TestProbe()
  val route = TopicApi.route(probe.ref)

  "Requires topic name" in {
    Post("/", FormData(Map("Action" -> "CreateTopic"))) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "Validates topic name" in {
    Post("/", FormData(Map("Action" -> "CreateTopic", "Name" -> "f$$"))) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "TopicDelete validates topic name" in {
    Post("/", FormData(Map("Action" -> "DeleteTopic", "TopicArn" -> "f$$"))) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "Sends create command to actor" in {
    // run the request asynchronously so we can expect the probe message and reply
    val routeF: Future[Unit] = Future {
      Post("/", FormData(Map("Action" -> "CreateTopic", "Name" -> "foo"))) ~> route ~> check {
        // don't assert here; the probe reply is what completes the route
      }
    }

    val msg = probe.expectMsgType[CmdCreateTopic](3.seconds)
    probe.reply(new Topic("arn:aws:sns:us-east-1:123456789012:foo", "foo"))

    Await.result(routeF, 5.seconds)
  }

  "Sends delete command to actor" in {
    val routeF: Future[Unit] = Future {
      Post("/", FormData(Map("Action" -> "DeleteTopic", "TopicArn" -> "arn-foo"))) ~> route ~> check {
      }
    }

    val msg = probe.expectMsgType[CmdDeleteTopic](3.seconds)
    probe.reply(org.apache.pekko.actor.Status.Success)

    Await.result(routeF, 5.seconds)
  }
}
