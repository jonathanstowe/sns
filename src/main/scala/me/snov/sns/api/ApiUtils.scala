package me.snov.sns.api

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.model.HttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ApiUtils {
  def completeFromFuture[T](fut: Future[T])(onSuccess: T => Route)(onFailure: Throwable => Route)(implicit ec: ExecutionContext): Route =
    onComplete(fut) {
      case Success(value) => onSuccess(value)
      case Failure(ex)    => onFailure(ex)
    }

  def completeFromFutureOr500[T](fut: Future[T])(onSuccess: T => Route)(implicit ec: ExecutionContext): Route =
    completeFromFuture(fut)(onSuccess) { ex => complete(HttpResponse(500, entity = ex.getMessage)) }
}
