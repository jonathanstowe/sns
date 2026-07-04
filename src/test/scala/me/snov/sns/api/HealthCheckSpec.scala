package me.snov.sns.api

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HealthCheckSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "Health check should return OK" in {
    Get("/health") ~> HealthCheckApi.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }
  }
}
